package uz.company.lunchbot.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.request.OpenOrderSessionRequest;
import uz.company.lunchbot.dto.request.RecalculateSessionRequest;
import uz.company.lunchbot.dto.response.SessionRecalculationResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.RecalculationMode;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.DuplicateActiveSessionException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.exception.OrderSessionClosedException;
import uz.company.lunchbot.repository.OrderSessionRepository;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.calculation.CalculationResult;
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
        if (!restaurant.isActive()) {
            throw new BadRequestException("Inactive restaurant cannot be used for a new session");
        }

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
        session.setDeliveryPrice(resolveOpeningDeliveryPrice(restaurant));
        session.setOpenedAt(LocalDateTime.now(clock));
        session.setDeadlineAt(LocalDateTime.of(orderDate, deadlineTime));
        session.setCreatedBy(actor);

        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.SESSION_OPENED, saved.getId(), actorUserId, null, sessionState(saved));
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

        CalculationResult result = recalculateOrderSnapshots(session, RecalculationMode.DELIVERY_ONLY);
        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.SESSION_CLOSED, sessionId, actorUserId, OrderSessionStatus.OPEN, saved.getStatus());
        auditCalculation(saved, actorUserId, RecalculationMode.DELIVERY_ONLY, false, result);
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
        ensureSessionAllowsDeliveryChanges(session);
        BigDecimal normalizedAmount = requireNonNegative(amount, "Delivery price must be non-negative");
        BigDecimal oldValue = session.getDeliveryPrice();
        session.setDeliveryPrice(normalizedAmount);

        CalculationResult result = recalculateOrderSnapshots(session, RecalculationMode.DELIVERY_ONLY);
        OrderSession saved = orderSessionRepository.save(session);
        auditService.log(AuditAction.SESSION_DELIVERY_CHANGED, sessionId, actorUserId, oldValue,
                Map.of("deliveryPrice", normalizedAmount, "mode", RecalculationMode.DELIVERY_ONLY));
        auditCalculation(saved, actorUserId, RecalculationMode.DELIVERY_ONLY, false, result);
        return saved;
    }

    @Transactional
    public SessionRecalculationResponse recalculateSession(Long sessionId,
                                                           RecalculateSessionRequest request,
                                                           Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        OrderSession session = getRequired(sessionId);
        RecalculationMode mode = request.mode();
        boolean forceConfirmed = Boolean.TRUE.equals(request.forceConfirmed());
        validateRecalculation(session, mode, forceConfirmed, actorUserId);

        CalculationResult result = recalculateOrderSnapshots(session, mode);
        auditCalculation(session, actorUserId, mode, forceConfirmed, result);

        if (mode == RecalculationMode.FULL_PRICE_REBUILD) {
            auditService.log(AuditAction.FULL_PRICE_REBUILD, sessionId, actorUserId, null, calculationAuditState(mode, forceConfirmed, result));
        }
        if (session.getStatus() == OrderSessionStatus.CONFIRMED && forceConfirmed) {
            auditService.log(AuditAction.CONFIRMED_SESSION_FORCE_RECALCULATED,
                    sessionId, actorUserId, null, calculationAuditState(mode, true, result));
        }

        return new SessionRecalculationResponse(
                sessionId,
                mode,
                result.orderedCount(),
                result.totalFoodAmount(),
                result.totalContainerAmount(),
                result.deliveryPrice(),
                result.totalFinalAmount(),
                result.roundingDifference(),
                LocalDateTime.now(clock));
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

    private BigDecimal resolveOpeningDeliveryPrice(Restaurant restaurant) {
        if (!restaurant.isDeliveryEnabled()) {
            return BigDecimal.ZERO;
        }
        return restaurant.getDefaultDeliveryPrice() == null ? BigDecimal.ZERO : restaurant.getDefaultDeliveryPrice();
    }

    private CalculationResult recalculateOrderSnapshots(OrderSession session, RecalculationMode mode) {
        List<UserOrder> orders = userOrderRepository.findAllByOrderSessionId(session.getId());
        CalculationResult result = orderCalculationService.recalculate(session, orders, mode);
        userOrderRepository.saveAll(orders);
        log.info("session_calculation_rebuilt sessionId={} orderCount={} mode={}", session.getId(), orders.size(), mode);
        return result;
    }

    private void ensureSessionAllowsDeliveryChanges(OrderSession session) {
        if (session.getStatus() == OrderSessionStatus.CANCELLED) {
            throw new BadRequestException("Cancelled sessions cannot be repriced");
        }
        if (session.getStatus() == OrderSessionStatus.CONFIRMED) {
            throw new BadRequestException("Confirmed sessions cannot be repriced");
        }
    }

    private void validateRecalculation(OrderSession session,
                                       RecalculationMode mode,
                                       boolean forceConfirmed,
                                       Long actorUserId) {
        if (session.getStatus() == OrderSessionStatus.CANCELLED) {
            throw new BadRequestException("Cancelled sessions cannot be recalculated");
        }
        if (forceConfirmed && session.getStatus() != OrderSessionStatus.CONFIRMED) {
            throw new BadRequestException("forceConfirmed can only be used for confirmed sessions");
        }
        if (mode == RecalculationMode.FULL_PRICE_REBUILD) {
            adminAccessService.ensureSuperAdmin(actorUserId);
        }
        if (session.getStatus() == OrderSessionStatus.CONFIRMED) {
            if (!forceConfirmed) {
                throw new BadRequestException("Confirmed sessions require forceConfirmed=true for recalculation");
            }
            adminAccessService.ensureSuperAdmin(actorUserId);
        } else if (forceConfirmed) {
            adminAccessService.ensureSuperAdmin(actorUserId);
        }
    }

    private void ensureSessionStatus(OrderSession session, OrderSessionStatus expectedStatus, String message) {
        if (session.getStatus() != expectedStatus) {
            throw new BadRequestException(message);
        }
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private void auditCalculation(OrderSession session,
                                  Long actorUserId,
                                  RecalculationMode mode,
                                  boolean forceConfirmed,
                                  CalculationResult result) {
        auditService.log(AuditAction.CALCULATION_REBUILT,
                session.getId(),
                actorUserId,
                null,
                calculationAuditState(mode, forceConfirmed, result));
    }

    private Map<String, Object> calculationAuditState(RecalculationMode mode,
                                                      boolean forceConfirmed,
                                                      CalculationResult result) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("mode", mode);
        state.put("forceConfirmed", forceConfirmed);
        state.put("orderedCount", result.orderedCount());
        state.put("totalFoodAmount", result.totalFoodAmount());
        state.put("totalContainerAmount", result.totalContainerAmount());
        state.put("deliveryPrice", result.deliveryPrice());
        state.put("totalFinalAmount", result.totalFinalAmount());
        state.put("roundingDifference", result.roundingDifference());
        return state;
    }

    private Map<String, Object> sessionState(OrderSession session) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sessionId", session.getId());
        state.put("restaurantId", session.getRestaurant().getId());
        state.put("orderDate", session.getOrderDate());
        state.put("status", session.getStatus());
        state.put("deliveryPrice", session.getDeliveryPrice());
        state.put("deadlineAt", session.getDeadlineAt());
        return state;
    }
}
