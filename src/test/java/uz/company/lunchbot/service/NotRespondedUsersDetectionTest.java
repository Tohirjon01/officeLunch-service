package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.service.calculation.OrderCalculationService;

class NotRespondedUsersDetectionTest {

    @Test
    void shouldExcludeSkippedUsersFromNonResponders() {
        UserOrderRepository userOrderRepository = mock(UserOrderRepository.class);
        UserService userService = mock(UserService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        OrderCalculationService calculationService = mock(OrderCalculationService.class);
        AuditService auditService = mock(AuditService.class);
        uz.company.lunchbot.security.AdminAccessService adminAccessService = mock(uz.company.lunchbot.security.AdminAccessService.class);

        UserOrderService service = new UserOrderService(
                userOrderRepository,
                userService,
                menuItemService,
                orderSessionService,
                calculationService,
                auditService,
                adminAccessService,
                Clock.fixed(Instant.parse("2026-05-04T07:00:00Z"), ZoneId.of("UTC")));

        LunchUser approved1 = user(1L, "Bobur");
        LunchUser approved2 = user(2L, "Ravshan");
        LunchUser approved3 = user(3L, "Jamshid");

        OrderSession session = new OrderSession();
        session.setId(100L);

        UserOrder order1 = new UserOrder();
        order1.setUser(approved1);

        UserOrder order2 = new UserOrder();
        order2.setUser(approved2);

        when(userService.getApprovedUsers()).thenReturn(List.of(approved1, approved2, approved3));
        when(userOrderRepository.findAllByOrderSessionId(100L)).thenReturn(List.of(order1, order2));

        List<LunchUser> result = service.findNotRespondedUsers(session);

        assertThat(result).extracting(LunchUser::getId).containsExactly(3L);
    }

    private static LunchUser user(Long id, String firstName) {
        LunchUser user = new LunchUser();
        user.setId(id);
        user.setFirstName(firstName);
        return user;
    }
}
