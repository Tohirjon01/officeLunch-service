package uz.company.lunchbot.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.request.OpenOrderSessionRequest;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.DuplicateActiveSessionException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.exception.OrderSessionClosedException;
import uz.company.lunchbot.repository.OrderSessionRepository;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.calculation.OrderCalculationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSessionService {

    private final OrderSessionRepository orderSessionRepository;
    private final UserOrderRepository userOrderRepository;
    private final RestaurantService restaurantService;
    private final OrderCalculationService orderCalculationService;
    private final AuditService auditService;
    private final AdminAccessService adminAccessService;
    private final LunchProperties lunchProperties;
    private final Clock clock;

    public OrderSession getRequired(Long id) {
        return orderSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order session not found: " + id));
    }

    public Optional<OrderSession> getTodaySession() {
        Restaurant restaurant = restaurantService.getDefaultActiveRestaurant();
        return orderSessionRepository.findByRestaurantIdAndOrderDate(restaurant.getId(), LocalDate.now(clock));
    }

    public Optional<OrderSession> getTodayOpenSession() {
        Restaurant restaurant = restaurantService.getDefaultActiveRestaurant();
        return orderSessionRepository.findByRestaurantIdAndOrderDateAndStatus(
                restaurant.getId(), LocalDate.now(clock), OrderSessionStatus.OPEN);
    }

    public OrderSession getActiveOrderingSession() {
        OrderSession session = getTodayOpenSession()
                .orElseThrow(() -> new OrderSessionClosedException("Today's order is already closed."));
        if (session.getDeadlineAt().isBefore(LocalDateTime.now(clock))) {
            throw new OrderSessionClosedException("Today's order is already closed.");
        }
        return session;
    }

    @Transactional
    public OrderSession openSession(OpenOrderSessionRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        Restaurant restaurant = request != null && request.restaurantId() != null
                ? restaurantService.getRequired(request.restaurantId())
                : restaurantService.getDefaultActiveRestaurant();

        LocalDate orderDate = request != null && request.orderDate() != null ? request.orderDate() : LocalDate.now(clock);
        LocalTime deadlineTime = request != null && request.deadlineTime() != null
                ? request.deadlineTime()
                : lunchProperties.defaultDeadlineTime();

        orderSessionRepository.findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(restaurant.getId(), OrderSessionStatus.OPEN)
                .ifPresent(existing -> {
                    throw new DuplicateActiveSessionException("An open session already exists for restaurant " + restaurant.getName());
                });

        if (orderSessionRepository.findByRestaurantIdAndOrderDate(restaurant.getId(), orderDate).isPresent()) {
            throw new BadRequestException("Order session already exists for this restaurant and date");
        }

        LunchUser actor = adminAccessService.getActor(actorUserId);
        OrderSession session = new OrderSession();
        session.setRestaurant(restaurant);
        session.setOrderDate(orderDate);
        session.setStatus(OrderSessionStatus.OPEN);
        session.setDeliveryPrice(request != null && request.deliveryPrice() != null
                ? request.deliveryPrice()
                : lunchProperties.defaultDeliveryPrice());
        session.setContainerPrice(request != null && request.containerPrice() != null
                ? request.containerPrice()
                : lunchProperties.defaultContainerPrice());
        session.setOpenedAt(LocalDateTime.now(clock));
        session.setDeadlineAt(LocalDateTime.of(orderDate, deadlineTime));
        session.setCreatedBy(actor);

        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.SESSION_OPENED, saved.getId(), actorUserId, null, saved.getOrderDate());
        log.info("session_opened sessionId={} restaurantId={} orderDate={}", saved.getId(), restaurant.getId(), saved.getOrderDate());
        return saved;
    }

    @Transactional
    public OrderSession openTodayDefaultSessionIfAbsent() {
        Optional<OrderSession> existing = getTodaySession();
        if (existing.isPresent()) {
            return existing.get();
        }
        return openSession(null, null);
    }

    @Transactional
    public OrderSession closeSession(Long sessionId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        OrderSession session = getRequired(sessionId);
        ensureSessionStatus(session, OrderSessionStatus.OPEN, "Only open sessions can be closed");
        session.setStatus(OrderSessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now(clock));
        recalculateSession(session);
        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.SESSION_CLOSED, sessionId, actorUserId, OrderSessionStatus.OPEN, saved.getStatus());
        return saved;
    }

    @Transactional
    public OrderSession confirmSession(Long sessionId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        OrderSession session = getRequired(sessionId);
        ensureSessionStatus(session, OrderSessionStatus.CLOSED, "Only closed sessions can be confirmed");
        session.setStatus(OrderSessionStatus.CONFIRMED);
        session.setConfirmedAt(LocalDateTime.now(clock));
        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.SESSION_CONFIRMED, sessionId, actorUserId, OrderSessionStatus.CLOSED, saved.getStatus());
        return saved;
    }

    @Transactional
    public OrderSession cancelSession(Long sessionId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        OrderSession session = getRequired(sessionId);
        if (session.getStatus() == OrderSessionStatus.CONFIRMED) {
            throw new BadRequestException("Confirmed sessions cannot be cancelled");
        }
        OrderSessionStatus oldStatus = session.getStatus();
        session.setStatus(OrderSessionStatus.CANCELLED);
        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.SESSION_CANCELLED, sessionId, actorUserId, oldStatus, saved.getStatus());
        return saved;
    }

    @Transactional
    public OrderSession extendSession(Long sessionId, int minutes, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        OrderSession session = getRequired(sessionId);
        if (session.getStatus() == OrderSessionStatus.CONFIRMED || session.getStatus() == OrderSessionStatus.CANCELLED) {
            throw new BadRequestException("This session can no longer be extended");
        }
        OrderSessionStatus oldStatus = session.getStatus();
        session.setStatus(OrderSessionStatus.OPEN);
        session.setClosedAt(null);
        session.setDeadlineAt(session.getDeadlineAt().plusMinutes(minutes));
        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.SESSION_EXTENDED, sessionId, actorUserId, oldStatus, saved.getDeadlineAt());
        return saved;
    }

    @Transactional
    public OrderSession updateDeliveryPrice(Long sessionId, BigDecimal amount, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        OrderSession session = getRequired(sessionId);
        BigDecimal oldValue = session.getDeliveryPrice();
        session.setDeliveryPrice(amount);
        recalculateSession(session);
        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.DELIVERY_CHANGED, sessionId, actorUserId, oldValue, amount);
        return saved;
    }

    @Transactional
    public OrderSession updateContainerPrice(Long sessionId, BigDecimal amount, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        OrderSession session = getRequired(sessionId);
        BigDecimal oldValue = session.getContainerPrice();
        session.setContainerPrice(amount);
        recalculateSession(session);
        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.CONTAINER_CHANGED, sessionId, actorUserId, oldValue, amount);
        return saved;
    }

    public void ensureUserCanPlaceOrder(OrderSession session) {
        if (session.getStatus() != OrderSessionStatus.OPEN || session.getDeadlineAt().isBefore(LocalDateTime.now(clock))) {
            throw new OrderSessionClosedException("Today's order is already closed.");
        }
    }

    public void ensureAdminCanEditOrders(OrderSession session) {
        if (session.getStatus() == OrderSessionStatus.CONFIRMED || session.getStatus() == OrderSessionStatus.CANCELLED) {
            throw new BadRequestException("Orders cannot be edited for confirmed or cancelled sessions");
        }
    }

    private void recalculateSession(OrderSession session) {
        List<UserOrder> orders = userOrderRepository.findAllByOrderSessionId(session.getId());
        orderCalculationService.recalculate(session, orders);
        userOrderRepository.saveAll(orders);
        auditService.log(AuditAction.CALCULATION_REBUILT, session.getId(), null, null, orders.size());
        log.info("session_calculation_rebuilt sessionId={} orderCount={}", session.getId(), orders.size());
    }

    private void ensureSessionStatus(OrderSession session, OrderSessionStatus expectedStatus, String message) {
        if (session.getStatus() != expectedStatus) {
            throw new BadRequestException(message);
        }
    }
}
