package uz.company.lunchbot.service.impl;

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
import uz.company.lunchbot.service.AuditService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.PaymentService;
import uz.company.lunchbot.service.RestaurantService;
import uz.company.lunchbot.service.calculation.CalculationResult;
import uz.company.lunchbot.service.calculation.OrderCalculationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSessionServiceImpl implements OrderSessionService {

    private final OrderSessionRepository orderSessionRepository;
    private final UserOrderRepository userOrderRepository;
    private final RestaurantService restaurantService;
    private final OrderCalculationService orderCalculationService;
    private final AuditService auditService;
    private final PaymentService paymentService;
    private final AdminAccessService adminAccessService;
    private final LunchProperties lunchProperties;
    private final Clock clock;

    @Override
    public OrderSession getRequired(Long id) {
        return orderSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order session not found: " + id));
    }

    @Override
    public Optional<OrderSession> getTodaySession() {
        return orderSessionRepository.findFirstByOrderDateOrderByCreatedAtDesc(LocalDate.now(clock));
    }

    @Override
    public Optional<OrderSession> getTodayOpenSession() {
        return orderSessionRepository.findFirstByOrderDateAndStatusOrderByCreatedAtDesc(
                LocalDate.now(clock),
                OrderSessionStatus.OPEN
        );
    }

    @Override
    public OrderSession getActiveOrderingSession() {
        OrderSession session = getTodayOpenSession()
                .orElseThrow(() -> new OrderSessionClosedException("Today's order is already closed."));

        if (session.getDeadlineAt().isBefore(LocalDateTime.now(clock))) {
            throw new OrderSessionClosedException("Today's order is already closed.");
        }

        return session;
    }

    @Override
    @Transactional
    public OrderSession openSession(OpenOrderSessionRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant restaurant = request != null && request.restaurantId() != null
                ? restaurantService.getRequired(request.restaurantId())
                : restaurantService.getDefaultActiveRestaurant();

        if (!restaurant.isActive()) {
            throw new BadRequestException("Inactive restaurant cannot be used for a new session");
        }

        LocalDate orderDate = request != null && request.orderDate() != null
                ? request.orderDate()
                : LocalDate.now(clock);

        cleanupStaleOpenSessions(orderDate, actorUserId);

        LocalTime deadlineTime = resolveDeadlineTime(request == null ? null : request.deadlineTime());

        orderSessionRepository
                .findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(
                        restaurant.getId(),
                        OrderSessionStatus.OPEN
                )
                .ifPresent(existing -> {
                    throw new DuplicateActiveSessionException(
                            "An open session already exists for restaurant " + restaurant.getName()
                    );
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

        /*
         * Important:
         * saveAndFlush is required here because audit_logs.order_session_id has a FK
         * to order_sessions.id. If we only call save(), Hibernate may delay INSERT
         * until transaction flush/commit. Then audit insert can happen before the
         * order_sessions row is visible in the database, causing FK violation.
         */
        OrderSession saved = orderSessionRepository.saveAndFlush(session);

        auditService.log(
                AuditAction.SESSION_OPENED,
                saved.getId(),
                actorUserId,
                null,
                sessionState(saved)
        );

        log.info(
                "session_opened sessionId={} restaurantId={} orderDate={}",
                saved.getId(),
                restaurant.getId(),
                saved.getOrderDate()
        );

        return saved;
    }

    @Override
    @Transactional
    public OrderSession openTodayDefaultSessionIfAbsent() {
        Optional<OrderSession> existing = getTodaySession();

        if (existing.isPresent()) {
            return existing.get();
        }

        return openSession(null, null);
    }

    @Override
    @Transactional
    public OrderSession openTodaySessionForRestaurantIfAbsent(Long restaurantId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant winnerRestaurant = restaurantService.getRequired(restaurantId);
        LocalDate today = LocalDate.now(clock);

        Optional<OrderSession> existingOpenSession = getTodayOpenSession();
        if (existingOpenSession.isPresent()) {
            OrderSession openSession = existingOpenSession.get();
            if (openSession.getRestaurant().getId().equals(winnerRestaurant.getId())) {
                return openSession;
            }

            Long oldRestaurantId = openSession.getRestaurant().getId();
            cancelOpenSession(openSession, actorUserId, true);
            log.warn(
                    "stale_order_session_closed oldSessionId={} oldRestaurantId={} winnerRestaurantId={}",
                    openSession.getId(),
                    oldRestaurantId,
                    winnerRestaurant.getId()
            );
        }

        return orderSessionRepository.findByRestaurantIdAndOrderDateAndStatus(
                        winnerRestaurant.getId(),
                        today,
                        OrderSessionStatus.OPEN
                )
                .orElseGet(() -> openSession(new OpenOrderSessionRequest(winnerRestaurant.getId(), today, null), actorUserId));
    }

    @Override
    @Transactional
    public OrderSession closeSession(Long sessionId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        OrderSession session = getRequired(sessionId);
        ensureSessionStatus(session, OrderSessionStatus.OPEN, "Only open sessions can be closed");

        return closeOpenSession(session, actorUserId, false);
    }

    private OrderSession closeOpenSession(OrderSession session, Long actorUserId, boolean staleCleanup) {
        LocalDateTime closedAt = LocalDateTime.now(clock);

        session.setStatus(OrderSessionStatus.CLOSED);
        session.setClosedAt(closedAt);

        CalculationResult result = recalculateOrderSnapshots(session, RecalculationMode.DELIVERY_ONLY);

        OrderSession saved = orderSessionRepository.save(session);

        auditService.log(
                AuditAction.SESSION_CLOSED,
                saved.getId(),
                actorUserId,
                OrderSessionStatus.OPEN,
                staleCleanup ? Map.of("status", saved.getStatus(), "staleCleanup", true, "closedAt", closedAt) : saved.getStatus()
        );

        auditCalculation(
                saved,
                actorUserId,
                RecalculationMode.DELIVERY_ONLY,
                false,
                result
        );

        paymentService.initializePaymentsForClosedSession(saved);

        if (staleCleanup) {
            log.warn("stale_open_session_closed sessionId={} orderDate={}", saved.getId(), saved.getOrderDate());
        }

        return saved;
    }

    @Override
    @Transactional
    public OrderSession confirmSession(Long sessionId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        OrderSession session = getRequired(sessionId);
        ensureSessionStatus(session, OrderSessionStatus.CLOSED, "Only closed sessions can be confirmed");

        session.setStatus(OrderSessionStatus.CONFIRMED);
        session.setConfirmedAt(LocalDateTime.now(clock));

        OrderSession saved = orderSessionRepository.save(session);

        auditService.log(
                AuditAction.SESSION_CONFIRMED,
                sessionId,
                actorUserId,
                OrderSessionStatus.CLOSED,
                saved.getStatus()
        );

        return saved;
    }

    @Override
    @Transactional
    public OrderSession cancelSession(Long sessionId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        OrderSession session = getRequired(sessionId);

        if (session.getStatus() == OrderSessionStatus.CONFIRMED) {
            throw new BadRequestException("Confirmed sessions cannot be cancelled");
        }

        return cancelOpenSession(session, actorUserId, false);
    }

    private OrderSession cancelOpenSession(OrderSession session, Long actorUserId, boolean staleCleanup) {
        if (session.getStatus() == OrderSessionStatus.CONFIRMED) {
            throw new BadRequestException("Confirmed sessions cannot be cancelled");
        }

        OrderSessionStatus oldStatus = session.getStatus();
        session.setStatus(OrderSessionStatus.CANCELLED);

        OrderSession saved = orderSessionRepository.save(session);

        auditService.log(
                AuditAction.SESSION_CANCELLED,
                saved.getId(),
                actorUserId,
                oldStatus,
                staleCleanup ? Map.of("status", saved.getStatus(), "staleCleanup", true) : saved.getStatus()
        );

        if (staleCleanup) {
            log.warn("stale_future_session_cancelled sessionId={} orderDate={}", saved.getId(), saved.getOrderDate());
        }

        return saved;
    }

    @Override
    @Transactional
    public OrderSession extendSession(Long sessionId, int minutes, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        OrderSession session = getRequired(sessionId);

        if (session.getStatus() == OrderSessionStatus.CONFIRMED
                || session.getStatus() == OrderSessionStatus.CANCELLED) {
            throw new BadRequestException("This session can no longer be extended");
        }

        OrderSessionStatus oldStatus = session.getStatus();

        session.setStatus(OrderSessionStatus.OPEN);
        session.setClosedAt(null);
        session.setDeadlineAt(session.getDeadlineAt().plusMinutes(minutes));

        OrderSession saved = orderSessionRepository.save(session);

        auditService.log(
                AuditAction.SESSION_EXTENDED,
                sessionId,
                actorUserId,
                oldStatus,
                saved.getDeadlineAt()
        );

        log.info(
                "order_deadline_extended sessionId={} minutes={} newDeadline={}",
                saved.getId(),
                minutes,
                saved.getDeadlineAt()
        );

        return saved;
    }

    @Override
    @Transactional
    public OrderSession reduceDeadline(Long sessionId, int minutes, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        OrderSession session = getRequired(sessionId);
        if (session.getStatus() != OrderSessionStatus.OPEN) {
            throw new BadRequestException("Bu order allaqachon yopilgan.");
        }
        if (minutes <= 0) {
            throw new BadRequestException("Minutes must be greater than zero");
        }

        LocalDateTime newDeadline = session.getDeadlineAt().minusMinutes(minutes);
        if (!newDeadline.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Deadline hozirgi vaqtdan oldin bo'lishi mumkin emas.");
        }

        session.setDeadlineAt(newDeadline);
        OrderSession saved = orderSessionRepository.save(session);

        log.info(
                "order_deadline_reduced sessionId={} minutes={} newDeadline={}",
                saved.getId(),
                minutes,
                saved.getDeadlineAt()
        );

        return saved;
    }

    @Override
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

        auditService.log(
                AuditAction.SESSION_DELIVERY_CHANGED,
                sessionId,
                actorUserId,
                oldValue,
                Map.of(
                        "deliveryPrice", normalizedAmount,
                        "mode", RecalculationMode.DELIVERY_ONLY
                )
        );

        auditCalculation(
                saved,
                actorUserId,
                RecalculationMode.DELIVERY_ONLY,
                false,
                result
        );

        return saved;
    }

    @Override
    @Transactional
    public SessionRecalculationResponse recalculateSession(
            Long sessionId,
            RecalculateSessionRequest request,
            Long actorUserId
    ) {
        adminAccessService.ensureAdmin(actorUserId);

        OrderSession session = getRequired(sessionId);

        RecalculationMode mode = request.mode();
        boolean forceConfirmed = Boolean.TRUE.equals(request.forceConfirmed());

        validateRecalculation(session, mode, forceConfirmed, actorUserId);

        CalculationResult result = recalculateOrderSnapshots(session, mode);

        auditCalculation(
                session,
                actorUserId,
                mode,
                forceConfirmed,
                result
        );

        if (mode == RecalculationMode.FULL_PRICE_REBUILD) {
            auditService.log(
                    AuditAction.FULL_PRICE_REBUILD,
                    sessionId,
                    actorUserId,
                    null,
                    calculationAuditState(mode, forceConfirmed, result)
            );
        }

        if (session.getStatus() == OrderSessionStatus.CONFIRMED && forceConfirmed) {
            auditService.log(
                    AuditAction.CONFIRMED_SESSION_FORCE_RECALCULATED,
                    sessionId,
                    actorUserId,
                    null,
                    calculationAuditState(mode, true, result)
            );
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
                LocalDateTime.now(clock)
        );
    }

    @Override
    public void ensureUserCanPlaceOrder(OrderSession session) {
        if (session.getStatus() != OrderSessionStatus.OPEN
                || session.getDeadlineAt().isBefore(LocalDateTime.now(clock))) {
            throw new OrderSessionClosedException("Today's order is already closed.");
        }
    }

    @Override
    public void ensureAdminCanEditOrders(OrderSession session) {
        if (session.getStatus() == OrderSessionStatus.CONFIRMED
                || session.getStatus() == OrderSessionStatus.CANCELLED) {
            throw new BadRequestException("Orders cannot be edited for confirmed or cancelled sessions");
        }
    }

    private BigDecimal resolveOpeningDeliveryPrice(Restaurant restaurant) {
        if (!restaurant.isDeliveryEnabled()) {
            return BigDecimal.ZERO;
        }

        return restaurant.getDefaultDeliveryPrice() == null
                ? BigDecimal.ZERO
                : restaurant.getDefaultDeliveryPrice();
    }

    private void cleanupStaleOpenSessions(LocalDate targetDate, Long actorUserId) {
        if (!targetDate.equals(LocalDate.now(clock))) {
            return;
        }

        orderSessionRepository.findAllByStatusOrderByOrderDateAscCreatedAtAsc(OrderSessionStatus.OPEN).stream()
                .filter(existing -> !existing.getOrderDate().equals(targetDate))
                .forEach(existing -> {
                    if (existing.getOrderDate().isBefore(targetDate)) {
                        closeOpenSession(existing, actorUserId, true);
                        return;
                    }
                    cancelOpenSession(existing, actorUserId, true);
                });
    }

    private LocalTime resolveDeadlineTime(LocalTime requestedDeadlineTime) {
        if (requestedDeadlineTime != null) {
            return requestedDeadlineTime;
        }

        if (lunchProperties.defaultDeadlineTime() != null) {
            return lunchProperties.defaultDeadlineTime();
        }

        LocalTime schedulerCloseTime = parseTimeFromCron(lunchProperties.scheduler() == null ? null : lunchProperties.scheduler().closeCron());
        return schedulerCloseTime == null ? LocalTime.of(11, 30) : schedulerCloseTime;
    }

    private LocalTime parseTimeFromCron(String cron) {
        if (cron == null || cron.isBlank()) {
            return null;
        }

        String[] parts = cron.trim().split("\\s+");
        if (parts.length < 3) {
            log.warn("scheduler_close_cron_parse_failed cron={}", cron);
            return null;
        }

        try {
            return LocalTime.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException exception) {
            log.warn("scheduler_close_cron_parse_failed cron={} reason={}", cron, exception.getMessage());
            return null;
        }
    }

    private CalculationResult recalculateOrderSnapshots(OrderSession session, RecalculationMode mode) {
        List<UserOrder> orders = userOrderRepository.findAllByOrderSessionId(session.getId());

        CalculationResult result = orderCalculationService.recalculate(
                session,
                orders,
                mode
        );

        userOrderRepository.saveAll(orders);

        log.info(
                "session_calculation_rebuilt sessionId={} orderCount={} mode={}",
                session.getId(),
                orders.size(),
                mode
        );

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

    private void validateRecalculation(
            OrderSession session,
            RecalculationMode mode,
            boolean forceConfirmed,
            Long actorUserId
    ) {
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

    private void ensureSessionStatus(
            OrderSession session,
            OrderSessionStatus expectedStatus,
            String message
    ) {
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

    private void auditCalculation(
            OrderSession session,
            Long actorUserId,
            RecalculationMode mode,
            boolean forceConfirmed,
            CalculationResult result
    ) {
        auditService.log(
                AuditAction.CALCULATION_REBUILT,
                session.getId(),
                actorUserId,
                null,
                calculationAuditState(mode, forceConfirmed, result)
        );
    }

    private Map<String, Object> calculationAuditState(
            RecalculationMode mode,
            boolean forceConfirmed,
            CalculationResult result
    ) {
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
