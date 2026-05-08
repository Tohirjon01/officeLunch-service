package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.response.PaymentResponse;
import uz.company.lunchbot.dto.response.PaymentSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Payment;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.PaymentMethod;
import uz.company.lunchbot.enums.PaymentRecordStatus;
import uz.company.lunchbot.enums.PaymentStatus;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.repository.PaymentRepository;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.impl.PaymentServiceImpl;

class PaymentServiceTest {

    @Test
    void shouldCreatePaymentRowsOnClose() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        UserService userService = mock(UserService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentRepository,
                userOrderRepository,
                userService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                clock()
        );

        OrderSession session = session(10L);
        UserOrder order = orderedUserOrder(20L, session, 11L, "Ali", "Assorti", "52000");
        when(userOrderRepository.findAllByOrderSessionIdAndStatus(10L, UserOrderStatus.ORDERED)).thenReturn(List.of(order));
        when(paymentRepository.findByUserOrderId(20L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", 30L);
            return payment;
        });

        service.initializePaymentsForClosedSession(session);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendPrivateText(eq(2011L), messageCaptor.capture(), any());
        assertThat(messageCaptor.getValue())
                .contains("Restoran: Osh Posh")
                .contains("Payment Instructions:")
                .contains("Card Number: 8600 1111 2222 3333")
                .contains("Card Owner: Toxirjon Sadullayev")
                .contains("Upload Receipt");
    }

    @Test
    void shouldUpdateStatusWhenReceiptAndCashAreSubmitted() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        UserService userService = mock(UserService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentRepository,
                userOrderRepository,
                userService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                clock()
        );

        LunchUser user = user(11L, 1001L, "Ali", 500L);
        LunchUser admin = user(12L, 2002L, "Admin", 600L);
        when(userService.getApprovedUserByTelegramUserId(1001L)).thenReturn(user);
        when(userService.getApprovedAdmins()).thenReturn(List.of(admin));

        Payment payment = waitingPayment(40L, session(10L), user, "52000");
        when(paymentRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.submitReceipt(1001L, "file-1", 777L);
        assertThat(payment.getStatus()).isEqualTo(PaymentRecordStatus.WAITING_APPROVAL);
        verify(notificationService).copyPrivateMessage(600L, 500L, 777L);

        payment.setStatus(PaymentRecordStatus.WAITING_PAYMENT);
        when(paymentRepository.findById(40L)).thenReturn(Optional.of(payment));
        service.declareCashPayment(1001L, 40L);
        assertThat(payment.getStatus()).isEqualTo(PaymentRecordStatus.CASH_DECLARED);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void shouldRejectAndMarkCashPaymentAsPaid() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        UserService userService = mock(UserService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentRepository,
                userOrderRepository,
                userService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                clock()
        );

        LunchUser user = user(11L, 1001L, "Ali", 500L);
        Payment payment = waitingPayment(60L, session(10L), user, "52000");
        when(paymentRepository.findById(60L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse rejected = service.rejectPayment(60L, 7L);
        assertThat(rejected.status()).isEqualTo(PaymentRecordStatus.REJECTED);
        assertThat(payment.getUserOrder().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);

        payment.setStatus(PaymentRecordStatus.CASH_DECLARED);
        PaymentResponse paid = service.markCashPaid(60L, 7L);
        assertThat(paid.status()).isEqualTo(PaymentRecordStatus.PAID);
        assertThat(paid.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(payment.getUserOrder().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void shouldApprovePaymentAndBuildSummary() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        UserService userService = mock(UserService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentRepository,
                userOrderRepository,
                userService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                clock()
        );

        LunchUser user = user(11L, 1001L, "Ali", 500L);
        Payment payment = waitingPayment(50L, session(10L), user, "52000");
        when(paymentRepository.findById(50L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse approved = service.approvePayment(50L, 7L);
        assertThat(approved.status()).isEqualTo(PaymentRecordStatus.PAID);
        assertThat(payment.getUserOrder().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        Payment waiting = waitingPayment(51L, session(10L), user(12L, 1002L, "Vali", 501L), "48000");
        when(paymentRepository.findAllByOrderSessionIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(payment, waiting));

        PaymentSummaryResponse summary = service.getPaymentSummary(10L, 7L);
        assertThat(summary.totalExpectedAmount()).isEqualByComparingTo("100000");
        assertThat(summary.totalPaidAmount()).isEqualByComparingTo("52000");
        assertThat(summary.totalUnpaidAmount()).isEqualByComparingTo("48000");
        assertThat(service.buildPaymentSummaryText(10L, 7L)).contains("Ali — 52,000 UZS CARD");
    }

    private static Payment waitingPayment(Long id, OrderSession session, LunchUser user, String amount) {
        UserOrder order = orderedUserOrder(id + 100, session, user.getId(), user.getDisplayName(), "Assorti", amount);
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", id);
        payment.setUserOrder(order);
        payment.setUser(user);
        payment.setOrderSession(session);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setStatus(PaymentRecordStatus.WAITING_PAYMENT);
        return payment;
    }

    private static UserOrder orderedUserOrder(Long id, OrderSession session, Long userId, String name, String meal, String finalPrice) {
        LunchUser user = user(userId, userId + 1000, name, userId + 2000);
        uz.company.lunchbot.entity.MenuItem menuItem = new uz.company.lunchbot.entity.MenuItem();
        menuItem.setName(meal);

        UserOrder order = new UserOrder();
        ReflectionTestUtils.setField(order, "id", id);
        order.setOrderSession(session);
        order.setUser(user);
        order.setMenuItem(menuItem);
        order.setStatus(UserOrderStatus.ORDERED);
        order.setFoodPrice(new BigDecimal(finalPrice));
        order.setContainerPrice(new BigDecimal("2000"));
        order.setDeliveryShare(new BigDecimal("10000"));
        order.setFinalPrice(new BigDecimal(finalPrice));
        order.setQuantity(1);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        return order;
    }

    private static LunchUser user(Long id, Long telegramUserId, String firstName, Long chatId) {
        LunchUser user = new LunchUser();
        user.setId(id);
        user.setTelegramUserId(telegramUserId);
        user.setFirstName(firstName);
        user.setPrivateChatId(chatId);
        return user;
    }

    private static OrderSession session(Long id) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Osh Posh");

        OrderSession session = new OrderSession();
        session.setId(id);
        session.setRestaurant(restaurant);
        session.setOrderDate(LocalDate.of(2026, 5, 5));
        session.setStatus(OrderSessionStatus.CLOSED);
        session.setDeadlineAt(LocalDateTime.of(2026, 5, 5, 11, 30));
        return session;
    }

    private static Clock clock() {
        return Clock.fixed(Instant.parse("2026-05-05T12:00:00Z"), ZoneId.of("UTC"));
    }

    private static LunchProperties properties() {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                LocalTime.of(11, 30),
                uz.company.lunchbot.enums.RoundingStrategy.CEIL_TO_100,
                new LunchProperties.Scheduler(true, "", "", ""),
                new LunchProperties.RestaurantVoting(true, "", "", 5),
                new LunchProperties.Payment(true, "8600 1111 2222 3333", "Toxirjon Sadullayev", true),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L)
        );
    }
}
