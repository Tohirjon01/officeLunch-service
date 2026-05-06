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
import org.springframework.test.util.ReflectionTestUtils;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.request.OpenOrderSessionRequest;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.RecalculationMode;
import uz.company.lunchbot.enums.RoundingStrategy;
import uz.company.lunchbot.exception.DuplicateActiveSessionException;
import uz.company.lunchbot.repository.OrderSessionRepository;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.calculation.CalculationResult;
import uz.company.lunchbot.service.calculation.OrderCalculationService;
import uz.company.lunchbot.service.impl.OrderSessionServiceImpl;

class OrderSessionServiceTest {

    @Test
    void shouldRejectWhenOpenSessionAlreadyExists() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        PaymentService paymentService = mock(PaymentService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionServiceImpl service = new OrderSessionServiceImpl(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                paymentService,
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
        when(orderSessionRepository.findAllByStatusOrderByOrderDateAscCreatedAtAsc(OrderSessionStatus.OPEN)).thenReturn(List.of());
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
        PaymentService paymentService = mock(PaymentService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        Clock clock = Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC"));
        OrderSessionServiceImpl service = new OrderSessionServiceImpl(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                paymentService,
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
        when(orderSessionRepository.findAllByStatusOrderByOrderDateAscCreatedAtAsc(OrderSessionStatus.OPEN)).thenReturn(List.of());
        when(orderSessionRepository.findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(1L, OrderSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(orderSessionRepository.findByRestaurantIdAndOrderDate(1L, LocalDate.of(2026, 5, 4)))
                .thenReturn(Optional.empty());
        when(orderSessionRepository.saveAndFlush(any(OrderSession.class))).thenAnswer(invocation -> {
            OrderSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 10L);
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
        PaymentService paymentService = mock(PaymentService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionServiceImpl service = new OrderSessionServiceImpl(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                paymentService,
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
        when(calculationService.recalculate(session, List.of(order), RecalculationMode.DELIVERY_ONLY))
                .thenReturn(new CalculationResult(1, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(orderSessionRepository.save(any(OrderSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.closeSession(10L, 7L);

        verify(calculationService).recalculate(session, List.of(order), RecalculationMode.DELIVERY_ONLY);
    }

    @Test
    void shouldUseProvidedRestaurantWhenOpeningSession() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        PaymentService paymentService = mock(PaymentService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionServiceImpl service = new OrderSessionServiceImpl(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                paymentService,
                adminAccessService,
                properties(),
                Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC")));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setName("Tarnov");
        restaurant.setActive(true);
        restaurant.setDeliveryEnabled(false);

        when(restaurantService.getRequired(2L)).thenReturn(restaurant);
        when(orderSessionRepository.findAllByStatusOrderByOrderDateAscCreatedAtAsc(OrderSessionStatus.OPEN)).thenReturn(List.of());
        when(orderSessionRepository.findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(2L, OrderSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(orderSessionRepository.findByRestaurantIdAndOrderDate(2L, LocalDate.of(2026, 5, 4)))
                .thenReturn(Optional.empty());
        when(orderSessionRepository.saveAndFlush(any(OrderSession.class))).thenAnswer(invocation -> {
            OrderSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 20L);
            return session;
        });

        OrderSession opened = service.openSession(new OpenOrderSessionRequest(2L, null, null), 7L);

        assertThat(opened.getRestaurant().getId()).isEqualTo(2L);
        assertThat(opened.getDeliveryPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRejectInactiveRestaurantWhenOpeningSession() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        PaymentService paymentService = mock(PaymentService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionServiceImpl service = new OrderSessionServiceImpl(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                paymentService,
                adminAccessService,
                properties(),
                Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC")));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(3L);
        restaurant.setName("KFC");
        restaurant.setActive(false);

        when(restaurantService.getRequired(3L)).thenReturn(restaurant);

        assertThatThrownBy(() -> service.openSession(new OpenOrderSessionRequest(3L, null, null), 7L))
                .isInstanceOf(uz.company.lunchbot.exception.BadRequestException.class)
                .hasMessageContaining("Inactive restaurant cannot be used for a new session");
    }

    @Test
    void shouldCloseStaleOpenSessionBeforeOpeningToday() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        PaymentService paymentService = mock(PaymentService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionServiceImpl service = new OrderSessionServiceImpl(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                paymentService,
                adminAccessService,
                properties(),
                Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC")));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Default Restaurant");
        restaurant.setActive(true);
        restaurant.setDeliveryEnabled(true);
        restaurant.setDefaultDeliveryPrice(new BigDecimal("20000"));

        OrderSession stale = new OrderSession();
        stale.setId(90L);
        stale.setRestaurant(restaurant);
        stale.setOrderDate(LocalDate.of(2026, 5, 3));
        stale.setStatus(OrderSessionStatus.OPEN);
        stale.setDeliveryPrice(new BigDecimal("15000"));

        when(orderSessionRepository.findAllByStatusOrderByOrderDateAscCreatedAtAsc(OrderSessionStatus.OPEN)).thenReturn(List.of(stale));
        when(userOrderRepository.findAllByOrderSessionId(90L)).thenReturn(List.of());
        when(calculationService.recalculate(org.mockito.ArgumentMatchers.eq(stale), any(), org.mockito.ArgumentMatchers.eq(RecalculationMode.DELIVERY_ONLY)))
                .thenReturn(new CalculationResult(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(orderSessionRepository.save(any(OrderSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderSessionRepository.saveAndFlush(any(OrderSession.class))).thenAnswer(invocation -> {
            OrderSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 10L);
            return session;
        });
        when(restaurantService.getDefaultActiveRestaurant()).thenReturn(restaurant);
        when(orderSessionRepository.findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(1L, OrderSessionStatus.OPEN)).thenReturn(Optional.empty());
        when(orderSessionRepository.findByRestaurantIdAndOrderDate(1L, LocalDate.of(2026, 5, 4))).thenReturn(Optional.empty());

        service.openSession(null, 7L);

        assertThat(stale.getStatus()).isEqualTo(OrderSessionStatus.CLOSED);
        verify(paymentService).initializePaymentsForClosedSession(stale);
    }

    @Test
    void shouldUseSchedulerCloseTimeWhenDefaultDeadlineMissing() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        PaymentService paymentService = mock(PaymentService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionServiceImpl service = new OrderSessionServiceImpl(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                paymentService,
                adminAccessService,
                propertiesWithoutDefaultDeadline(),
                Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC")));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setName("Tarnov");
        restaurant.setActive(true);

        when(orderSessionRepository.findAllByStatusOrderByOrderDateAscCreatedAtAsc(OrderSessionStatus.OPEN)).thenReturn(List.of());
        when(restaurantService.getRequired(2L)).thenReturn(restaurant);
        when(orderSessionRepository.findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(2L, OrderSessionStatus.OPEN)).thenReturn(Optional.empty());
        when(orderSessionRepository.findByRestaurantIdAndOrderDate(2L, LocalDate.of(2026, 5, 4))).thenReturn(Optional.empty());
        when(orderSessionRepository.saveAndFlush(any(OrderSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderSession opened = service.openSession(new OpenOrderSessionRequest(2L, LocalDate.of(2026, 5, 4), null), 7L);

        assertThat(opened.getDeadlineAt().toLocalTime()).isEqualTo(LocalTime.of(11, 35));
    }

    @Test
    void shouldReplaceTodayOpenSessionForDifferentWinnerRestaurant() {
        OrderSessionRepository orderSessionRepository = mock(OrderSessionRepository.class);
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        PaymentService paymentService = mock(PaymentService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);

        OrderSessionServiceImpl service = new OrderSessionServiceImpl(
                orderSessionRepository,
                userOrderRepository,
                restaurantService,
                calculationService,
                auditService,
                paymentService,
                adminAccessService,
                properties(),
                Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC")));

        Restaurant oldRestaurant = new Restaurant();
        oldRestaurant.setId(1L);
        oldRestaurant.setName("Tarnov");
        oldRestaurant.setActive(true);

        Restaurant winnerRestaurant = new Restaurant();
        winnerRestaurant.setId(2L);
        winnerRestaurant.setName("Osh Posh");
        winnerRestaurant.setActive(true);

        OrderSession stale = new OrderSession();
        stale.setId(90L);
        stale.setRestaurant(oldRestaurant);
        stale.setOrderDate(LocalDate.of(2026, 5, 4));
        stale.setStatus(OrderSessionStatus.OPEN);

        when(restaurantService.getRequired(2L)).thenReturn(winnerRestaurant);
        when(orderSessionRepository.findFirstByOrderDateAndStatusOrderByCreatedAtDesc(LocalDate.of(2026, 5, 4), OrderSessionStatus.OPEN))
                .thenReturn(Optional.of(stale), Optional.empty());
        when(orderSessionRepository.findByRestaurantIdAndOrderDateAndStatus(2L, LocalDate.of(2026, 5, 4), OrderSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(userOrderRepository.findAllByOrderSessionId(90L)).thenReturn(List.of());
        when(calculationService.recalculate(org.mockito.ArgumentMatchers.eq(stale), any(), org.mockito.ArgumentMatchers.eq(RecalculationMode.DELIVERY_ONLY)))
                .thenReturn(new CalculationResult(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(orderSessionRepository.save(any(OrderSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderSessionRepository.findAllByStatusOrderByOrderDateAscCreatedAtAsc(OrderSessionStatus.OPEN)).thenReturn(List.of());
        when(orderSessionRepository.findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(2L, OrderSessionStatus.OPEN)).thenReturn(Optional.empty());
        when(orderSessionRepository.findByRestaurantIdAndOrderDate(2L, LocalDate.of(2026, 5, 4))).thenReturn(Optional.empty());
        when(orderSessionRepository.saveAndFlush(any(OrderSession.class))).thenAnswer(invocation -> {
            OrderSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 91L);
            return session;
        });

        OrderSession opened = service.openTodaySessionForRestaurantIfAbsent(2L, 7L);

        assertThat(stale.getStatus()).isEqualTo(OrderSessionStatus.CANCELLED);
        assertThat(opened.getRestaurant().getId()).isEqualTo(2L);
    }

    private static LunchProperties properties() {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                LocalTime.of(11, 30),
                RoundingStrategy.CEIL_TO_100,
                new LunchProperties.Scheduler(true, "", "", ""),
                new LunchProperties.RestaurantVoting(false, "", "", 5),
                new LunchProperties.Payment(true, "8600", "Owner", true),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L));
    }

    private static LunchProperties propertiesWithoutDefaultDeadline() {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                null,
                RoundingStrategy.CEIL_TO_100,
                new LunchProperties.Scheduler(true, "", "", "0 35 11 * * MON-FRI"),
                new LunchProperties.RestaurantVoting(false, "", "", 5),
                new LunchProperties.Payment(true, "8600", "Owner", true),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L));
    }
}
