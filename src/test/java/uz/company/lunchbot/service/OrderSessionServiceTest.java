package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.request.OpenOrderSessionRequest;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.RoundingStrategy;
import uz.company.lunchbot.exception.DuplicateActiveSessionException;
import uz.company.lunchbot.repository.OrderSessionRepository;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.calculation.OrderCalculationService;

class OrderSessionServiceTest {

    @Test
    void shouldRejectWhenOpenSessionAlreadyExists() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionService service = new OrderSessionService(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                adminAccessService,
                properties(),
                Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC")));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Default Restaurant");
        restaurant.setActive(true);

        OrderSession existing = new OrderSession();
        existing.setId(99L);
        existing.setStatus(OrderSessionStatus.OPEN);

        when(restaurantService.getDefaultActiveRestaurant()).thenReturn(restaurant);
        when(orderSessionRepository.findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(1L, OrderSessionStatus.OPEN))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.openSession(null, 7L))
                .isInstanceOf(DuplicateActiveSessionException.class);
    }

    @Test
    void shouldCopyRestaurantDeliveryPriceWhenOpeningSession() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        Clock clock = Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC"));
        OrderSessionService service = new OrderSessionService(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                adminAccessService,
                properties(),
                clock);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Default Restaurant");
        restaurant.setActive(true);
        restaurant.setDeliveryEnabled(true);
        restaurant.setDefaultDeliveryPrice(new BigDecimal("20000"));

        when(restaurantService.getDefaultActiveRestaurant()).thenReturn(restaurant);
        when(orderSessionRepository.findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(1L, OrderSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(orderSessionRepository.findByRestaurantIdAndOrderDate(1L, LocalDate.of(2026, 5, 4)))
                .thenReturn(Optional.empty());
        when(orderSessionRepository.save(any(OrderSession.class))).thenAnswer(invocation -> {
            OrderSession session = invocation.getArgument(0);
            session.setId(10L);
            return session;
        });

        OrderSession opened = service.openSession(new OpenOrderSessionRequest(null, null, null), 7L);

        assertThat(opened.getDeliveryPrice()).isEqualByComparingTo("20000");
        assertThat(opened.getStatus()).isEqualTo(OrderSessionStatus.OPEN);
    }

    @Test
    void shouldCloseSessionAndRecalculateOrders() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionService service = new OrderSessionService(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                adminAccessService,
                properties(),
                Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC")));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);

        OrderSession session = new OrderSession();
        session.setId(10L);
        session.setRestaurant(restaurant);
        session.setStatus(OrderSessionStatus.OPEN);
        session.setDeliveryPrice(new BigDecimal("30000"));

        UserOrder order = new UserOrder();
        order.setId(20L);

        when(orderSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(userOrderRepository.findAllByOrderSessionId(10L)).thenReturn(List.of(order));
        when(orderSessionRepository.save(any(OrderSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.closeSession(10L, 7L);

        verify(calculationService).recalculate(session, List.of(order));
    }

    private static LunchProperties properties() {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                LocalTime.of(11, 30),
                RoundingStrategy.CEIL_TO_100,
                new LunchProperties.Scheduler(true, "", "", ""),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L));
    }
}
