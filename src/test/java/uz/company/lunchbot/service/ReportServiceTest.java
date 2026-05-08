package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
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
import uz.company.lunchbot.service.impl.ReportServiceImpl;

class ReportServiceTest {

    @Test
    void shouldBuildCurrentSessionReport() {
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        ReportServiceImpl service = new ReportServiceImpl(
                orderSessionService,
                userOrderRepository,
                paymentRepository,
                adminAccessService
        );

        OrderSession session = session(10L);
        UserOrder aliOrder = orderedUserOrder(101L, session, 1L, "Ali", "Osh", "52000");
        UserOrder valiOrder = orderedUserOrder(102L, session, 2L, "Vali", "Salad", "48000");
        Payment aliPayment = payment(201L, aliOrder, PaymentRecordStatus.PAID, "52000");
        Payment valiPayment = payment(202L, valiOrder, PaymentRecordStatus.WAITING_APPROVAL, "48000");

        when(orderSessionService.getTodaySession()).thenReturn(Optional.of(session));
        when(userOrderRepository.findAllByOrderSessionId(10L)).thenReturn(List.of(aliOrder, valiOrder));
        when(paymentRepository.findAllByOrderSessionIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(aliPayment, valiPayment));

        String report = service.buildCurrentSessionReport(7L);

        assertThat(report)
                .contains("Kitchen View:")
                .contains("Osh: 1")
                .contains("Salad: 1")
                .contains("Total to collect: 100,000 UZS")
                .contains("Total approved: 52,000 UZS")
                .contains("Debtor List:")
                .contains("Vali");
    }

    private static Payment payment(Long id, UserOrder order, PaymentRecordStatus status, String amount) {
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", id);
        payment.setUserOrder(order);
        payment.setUser(order.getUser());
        payment.setOrderSession(order.getOrderSession());
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setStatus(status);
        return payment;
    }

    private static UserOrder orderedUserOrder(Long id, OrderSession session, Long userId, String name, String meal, String finalPrice) {
        LunchUser user = new LunchUser();
        user.setId(userId);
        user.setFirstName(name);

        MenuItem menuItem = new MenuItem();
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

    private static OrderSession session(Long id) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Osh Posh");

        OrderSession session = new OrderSession();
        session.setId(id);
        session.setRestaurant(restaurant);
        session.setOrderDate(LocalDate.of(2026, 5, 5));
        session.setStatus(OrderSessionStatus.CLOSED);
        return session;
    }
}
