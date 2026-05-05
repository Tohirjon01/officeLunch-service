package uz.company.lunchbot.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.dto.request.CreateManualOrderRequest;
import uz.company.lunchbot.dto.request.UpdateOrderRequest;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.enums.PaymentStatus;
import uz.company.lunchbot.enums.RecalculationMode;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.calculation.OrderCalculationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderService {

    private final UserOrderRepository userOrderRepository;
    private final UserService userService;
    private final MenuItemService menuItemService;
    private final OrderSessionService orderSessionService;
    private final OrderCalculationService orderCalculationService;
    private final AuditService auditService;
    private final AdminAccessService adminAccessService;
    private final Clock clock;

    public List<UserOrder> getOrdersForSession(Long sessionId) {
        orderSessionService.getRequired(sessionId);
        return userOrderRepository.findAllByOrderSessionId(sessionId);
    }

    public UserOrder getRequired(Long orderId) {
        return userOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("User order not found: " + orderId));
    }

    public Optional<UserOrder> getTodayOrderForTelegramUser(Long telegramUserId) {
        LunchUser user = userService.getRequiredByTelegramUserId(telegramUserId);
        return orderSessionService.getTodaySession()
                .flatMap(session -> userOrderRepository.findByOrderSessionIdAndUserId(session.getId(), user.getId()));
    }

    public List<LunchUser> findNotRespondedUsers(OrderSession session) {
        Set<Long> respondedUserIds = userOrderRepository.findAllByOrderSessionId(session.getId()).stream()
                .map(order -> order.getUser().getId())
                .collect(Collectors.toSet());

        return userService.getApprovedUsers().stream()
                .filter(user -> !respondedUserIds.contains(user.getId()))
                .toList();
    }

    @Transactional
    public UserOrder placeTodayOrder(Long telegramUserId, Long menuItemId) {
        LunchUser user = userService.getApprovedUserByTelegramUserId(telegramUserId);
        OrderSession session = orderSessionService.getActiveOrderingSession();
        MenuItem menuItem = validateMenuItemForSession(menuItemId, session);
        return saveOrderedRecord(session, user, menuItem, 1, user.getId());
    }

    @Transactional
    public UserOrder skipToday(Long telegramUserId) {
        LunchUser user = userService.getApprovedUserByTelegramUserId(telegramUserId);
        OrderSession session = orderSessionService.getActiveOrderingSession();
        UserOrder order = userOrderRepository.findByOrderSessionIdAndUserId(session.getId(), user.getId())
                .orElseGet(() -> createBaseOrder(session, user));
        UserOrderStatus oldStatus = order.getStatus();
        order.setMenuItem(null);
        order.setStatus(UserOrderStatus.SKIPPED);
        order.setQuantity(1);
        order.setDeliveryShare(BigDecimal.ZERO);
        order.setFinalPrice(BigDecimal.ZERO);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setOrderedAt(LocalDateTime.now(clock));
        UserOrder saved = userOrderRepository.save(order);
        recalculateSession(saved.getOrderSession());
        auditService.log(AuditAction.ORDER_SKIPPED, session.getId(), user.getId(), oldStatus, saved.getStatus());
        log.info("order_skipped userId={} sessionId={}", user.getId(), session.getId());
        return saved;
    }

    @Transactional
    public UserOrder createManualOrder(Long sessionId, CreateManualOrderRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        OrderSession session = orderSessionService.getRequired(sessionId);
        orderSessionService.ensureAdminCanEditOrders(session);
        LunchUser user = userService.getRequired(request.userId());
        MenuItem menuItem = validateMenuItemForSession(request.menuItemId(), session);
        return saveOrderedRecord(session, user, menuItem, request.quantity() == null ? 1 : request.quantity(), actorUserId, false);
    }

    @Transactional
    public UserOrder updateOrder(Long orderId, UpdateOrderRequest request, Long actorUserId) {
        UserOrder order = getRequired(orderId);
        orderSessionService.ensureAdminCanEditOrders(order.getOrderSession());
        if (request.status() == UserOrderStatus.SKIPPED) {
            return skipOrder(orderId, actorUserId);
        }
        if (request.status() == UserOrderStatus.CANCELLED) {
            return cancelOrder(orderId, actorUserId);
        }
        adminAccessService.ensureAdmin(actorUserId);
        if (request.menuItemId() == null) {
            throw new BadRequestException("menuItemId is required for ORDERED status");
        }
        MenuItem menuItem = validateMenuItemForSession(request.menuItemId(), order.getOrderSession());
        UserOrderStatus oldStatus = order.getStatus();
        applyOrderedSelection(order, menuItem, request.quantity() == null ? 1 : request.quantity());
        UserOrder saved = userOrderRepository.save(order);
        recalculateSession(saved.getOrderSession());
        auditService.log(AuditAction.ORDER_UPDATED, saved.getOrderSession().getId(), saved.getUser().getId(), oldStatus, saved.getStatus());
        return saved;
    }

    @Transactional
    public UserOrder skipOrder(Long orderId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        UserOrder order = getRequired(orderId);
        orderSessionService.ensureAdminCanEditOrders(order.getOrderSession());
        UserOrderStatus oldStatus = order.getStatus();
        order.setStatus(UserOrderStatus.SKIPPED);
        order.setMenuItem(null);
        order.setQuantity(1);
        order.setDeliveryShare(BigDecimal.ZERO);
        order.setFinalPrice(BigDecimal.ZERO);
        order.setOrderedAt(LocalDateTime.now(clock));
        UserOrder saved = userOrderRepository.save(order);
        recalculateSession(saved.getOrderSession());
        auditService.log(AuditAction.ORDER_SKIPPED, saved.getOrderSession().getId(), saved.getUser().getId(), oldStatus, saved.getStatus());
        return saved;
    }

    @Transactional
    public UserOrder cancelOrder(Long orderId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        UserOrder order = getRequired(orderId);
        orderSessionService.ensureAdminCanEditOrders(order.getOrderSession());
        UserOrderStatus oldStatus = order.getStatus();
        order.setStatus(UserOrderStatus.CANCELLED);
        order.setDeliveryShare(BigDecimal.ZERO);
        order.setFinalPrice(BigDecimal.ZERO);
        UserOrder saved = userOrderRepository.save(order);
        recalculateSession(saved.getOrderSession());
        auditService.log(AuditAction.ORDER_CANCELLED, saved.getOrderSession().getId(), saved.getUser().getId(), oldStatus, saved.getStatus());
        return saved;
    }

    @Transactional
    public UserOrder markPayment(Long orderId, PaymentStatus paymentStatus, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        UserOrder order = getRequired(orderId);
        order.setPaymentStatus(paymentStatus);
        return userOrderRepository.save(order);
    }

    private UserOrder saveOrderedRecord(OrderSession session, LunchUser user, MenuItem menuItem, int quantity, Long actorUserId) {
        return saveOrderedRecord(session, user, menuItem, quantity, actorUserId, true);
    }

    private UserOrder saveOrderedRecord(OrderSession session,
                                        LunchUser user,
                                        MenuItem menuItem,
                                        int quantity,
                                        Long actorUserId,
                                        boolean requireOpenSession) {
        if (requireOpenSession) {
            orderSessionService.ensureUserCanPlaceOrder(session);
        }
        UserOrder order = userOrderRepository.findByOrderSessionIdAndUserId(session.getId(), user.getId())
                .orElseGet(() -> createBaseOrder(session, user));
        UserOrderStatus oldStatus = order.getStatus();
        boolean created = order.getId() == null;
        applyOrderedSelection(order, menuItem, quantity);
        UserOrder saved = userOrderRepository.save(order);
        recalculateSession(session);
        auditService.log(created ? AuditAction.ORDER_CREATED : AuditAction.ORDER_UPDATED,
                session.getId(), user.getId(), oldStatus, saved.getMenuItem().getName());
        log.info("order_saved userId={} sessionId={} menuItemId={}", user.getId(), session.getId(), menuItem.getId());
        return saved;
    }

    private MenuItem validateMenuItemForSession(Long menuItemId, OrderSession session) {
        MenuItem menuItem = menuItemService.getRequired(menuItemId);
        if (!menuItem.isActive()) {
            throw new BadRequestException("Inactive menu item cannot be ordered");
        }
        if (!menuItem.getRestaurant().getId().equals(session.getRestaurant().getId())) {
            throw new BadRequestException("Menu item does not belong to the session restaurant");
        }
        return menuItem;
    }

    private UserOrder createBaseOrder(OrderSession session, LunchUser user) {
        UserOrder order = new UserOrder();
        order.setOrderSession(session);
        order.setUser(user);
        order.setQuantity(1);
        order.setFoodPrice(BigDecimal.ZERO);
        order.setContainerPrice(BigDecimal.ZERO);
        order.setDeliveryShare(BigDecimal.ZERO);
        order.setFinalPrice(BigDecimal.ZERO);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setOrderedAt(LocalDateTime.now(clock));
        order.setStatus(UserOrderStatus.SKIPPED);
        return order;
    }

    private void applyOrderedSelection(UserOrder order, MenuItem menuItem, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Order quantity must be greater than zero");
        }

        order.setMenuItem(menuItem);
        order.setStatus(UserOrderStatus.ORDERED);
        order.setQuantity(quantity);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setOrderedAt(LocalDateTime.now(clock));
        orderCalculationService.capturePriceSnapshot(order);
        order.setDeliveryShare(BigDecimal.ZERO);
        order.setFinalPrice(BigDecimal.ZERO);
    }

    private void recalculateSession(OrderSession session) {
        List<UserOrder> allOrders = userOrderRepository.findAllByOrderSessionId(session.getId());
        orderCalculationService.recalculate(session, allOrders, RecalculationMode.DELIVERY_ONLY);
        userOrderRepository.saveAll(allOrders);
    }
}
