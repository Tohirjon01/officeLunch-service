package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.PaymentStatus;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.service.calculation.OrderCalculationService;

class UserOrderServiceTest {

    @Test
    void shouldUpdateExistingOrderInsteadOfCreatingDuplicate() {
        var userOrderRepository = mock(uz.company.lunchbot.repository.UserOrderRepository.class);
        var userService = mock(UserService.class);
        var menuItemService = mock(MenuItemService.class);
        var orderSessionService = mock(OrderSessionService.class);
        var calculationService = mock(OrderCalculationService.class);
        var auditService = mock(AuditService.class);
        var adminAccessService = mock(uz.company.lunchbot.security.AdminAccessService.class);

        Clock clock = Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC"));

        UserOrderService service = new UserOrderService(
                userOrderRepository,
                userService,
                menuItemService,
                orderSessionService,
                calculationService,
                auditService,
                adminAccessService,
                clock);

        LunchUser user = new LunchUser();
        user.setId(11L);
        user.setTelegramUserId(1001L);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);

        OrderSession session = new OrderSession();
        session.setId(21L);
        session.setRestaurant(restaurant);
        session.setDeliveryPrice(new BigDecimal("30000"));
        session.setContainerPrice(new BigDecimal("2000"));

        MenuItem menuItem = new MenuItem();
        menuItem.setId(31L);
        menuItem.setRestaurant(restaurant);
        menuItem.setPrice(new BigDecimal("33000"));
        menuItem.setName("Bifteks");
        menuItem.setActive(true);

        UserOrder existing = new UserOrder();
        existing.setId(41L);
        existing.setUser(user);
        existing.setOrderSession(session);
        existing.setStatus(UserOrderStatus.ORDERED);
        existing.setFoodPrice(new BigDecimal("35000"));
        existing.setPaymentStatus(PaymentStatus.UNPAID);

        when(userService.getApprovedUserByTelegramUserId(1001L)).thenReturn(user);
        when(orderSessionService.getActiveOrderingSession()).thenReturn(session);
        doNothing().when(orderSessionService).ensureUserCanPlaceOrder(session);
        when(menuItemService.getRequired(31L)).thenReturn(menuItem);
        when(userOrderRepository.findByOrderSessionIdAndUserId(21L, 11L)).thenReturn(Optional.of(existing));
        when(userOrderRepository.save(any(UserOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userOrderRepository.findAllByOrderSessionId(21L)).thenReturn(List.of(existing));

        UserOrder result = service.placeTodayOrder(1001L, 31L);

        assertThat(result.getId()).isEqualTo(41L);
        assertThat(result.getMenuItem()).isEqualTo(menuItem);
        assertThat(result.getFoodPrice()).isEqualByComparingTo("33000");
        verify(userOrderRepository).save(existing);
        verify(calculationService).recalculate(session, List.of(existing));
    }
}
