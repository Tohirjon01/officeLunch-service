package uz.company.lunchbot.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.RestaurantVoteSession;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;
import uz.company.lunchbot.enums.RoundingStrategy;
import uz.company.lunchbot.service.NotificationService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.RestaurantVoteSessionService;
import uz.company.lunchbot.service.SummaryService;
import uz.company.lunchbot.service.UserOrderService;

class LunchSchedulerTest {

    @Test
    void shouldOpenRestaurantVotingWhenSchedulerRuns() {
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService voteSessionService = mock(RestaurantVoteSessionService.class);

        LunchScheduler scheduler = new LunchScheduler(
                properties(true),
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                voteSessionService
        );

        scheduler.openRestaurantVoting();

        verify(voteSessionService).openSession(null, null);
    }

    @Test
    void shouldCloseTodaysOpenRestaurantVotingWhenSchedulerRuns() {
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService voteSessionService = mock(RestaurantVoteSessionService.class);

        RestaurantVoteSession session = new RestaurantVoteSession();
        session.setId(15L);
        session.setStatus(RestaurantVoteSessionStatus.OPEN);
        when(voteSessionService.getTodaySession()).thenReturn(Optional.of(session));

        LunchScheduler scheduler = new LunchScheduler(
                properties(true),
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                voteSessionService
        );

        scheduler.closeRestaurantVoting();

        verify(voteSessionService).closeSession(15L, null);
    }

    @Test
    void shouldSendReminderForTodayOpenSession() {
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService voteSessionService = mock(RestaurantVoteSessionService.class);

        OrderSession session = new OrderSession();
        session.setId(20L);
        session.setStatus(OrderSessionStatus.OPEN);
        List<LunchUser> notRespondedUsers = List.of(new LunchUser());
        when(orderSessionService.getTodayOpenSession()).thenReturn(Optional.of(session));
        when(userOrderService.findNotRespondedUsers(session)).thenReturn(notRespondedUsers);

        LunchScheduler scheduler = new LunchScheduler(
                properties(true),
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                voteSessionService
        );

        scheduler.remindNotRespondedUsers();

        verify(notificationService).sendReminder(session, notRespondedUsers);
    }

    @Test
    void shouldCloseTodayOpenOrderSessionAndSendSummary() {
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService voteSessionService = mock(RestaurantVoteSessionService.class);

        OrderSession open = new OrderSession();
        open.setId(30L);
        OrderSession closed = new OrderSession();
        closed.setId(30L);
        SessionSummaryResponse summary = new SessionSummaryResponse(
                30L,
                null,
                OrderSessionStatus.CLOSED,
                0L,
                0L,
                0L,
                List.of(),
                List.of(),
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                "summary",
                "restaurant"
        );

        when(orderSessionService.getTodayOpenSession()).thenReturn(Optional.of(open));
        when(orderSessionService.closeSession(30L, null)).thenReturn(closed);
        when(summaryService.buildSummary(30L)).thenReturn(summary);

        LunchScheduler scheduler = new LunchScheduler(
                properties(true),
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                voteSessionService
        );

        scheduler.closeDailyLunchOrder();

        verify(notificationService).sendClosedSummary(summary, 30L);
    }

    @Test
    void shouldSkipDefaultOpenWhenRestaurantVotingIsEnabled() {
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService voteSessionService = mock(RestaurantVoteSessionService.class);

        LunchScheduler scheduler = new LunchScheduler(
                properties(true),
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                voteSessionService
        );

        scheduler.openDailyLunchOrder();

        verify(orderSessionService, never()).openTodayDefaultSessionIfAbsent();
    }

    private static LunchProperties properties(boolean votingEnabled) {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                LocalTime.of(11, 30),
                RoundingStrategy.CEIL_TO_100,
                new LunchProperties.Scheduler(true, "0 16 10 * * MON-FRI", "0 20 11 * * MON-FRI", "0 30 11 * * MON-FRI"),
                new LunchProperties.RestaurantVoting(votingEnabled, "0 30 9 * * MON-FRI", "0 15 10 * * MON-FRI", 45),
                new LunchProperties.Payment(true, "8600", "Owner", true),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L)
        );
    }
}
