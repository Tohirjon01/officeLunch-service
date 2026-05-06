package uz.company.lunchbot.scheduler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.response.RestaurantVoteSessionResponse;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.RestaurantVoteSession;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.service.NotificationService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.RestaurantVoteSessionService;
import uz.company.lunchbot.service.SummaryService;
import uz.company.lunchbot.service.UserOrderService;

@Slf4j
@Component
@RequiredArgsConstructor
public class LunchScheduler {

    private final LunchProperties lunchProperties;
    private final OrderSessionService orderSessionService;
    private final UserOrderService userOrderService;
    private final SummaryService summaryService;
    private final NotificationService telegramNotificationService;
    private final RestaurantVoteSessionService restaurantVoteSessionService;

    @Scheduled(cron = "${lunch.scheduler.open-cron:0 16 10 * * MON-FRI}", zone = "${lunch.timezone}")
    public void openDailyLunchOrder() {
        if (!lunchProperties.scheduler().enabled()) {
            return;
        }
        if (lunchProperties.restaurantVoting() != null && lunchProperties.restaurantVoting().enabled()) {
            return;
        }
        OrderSession session = orderSessionService.openTodayDefaultSessionIfAbsent();
        if (session.getStatus().name().equals("OPEN")) {
            telegramNotificationService.sendGroupOpenAnnouncement(session);
            log.info("scheduler_open_session sessionId={}", session.getId());
        }
    }

    @Scheduled(cron = "${lunch.scheduler.reminder-cron}", zone = "${lunch.timezone}")
    public void remindNotRespondedUsers() {
        if (!lunchProperties.scheduler().enabled()) {
            return;
        }
        orderSessionService.getTodayOpenSession().ifPresent(session -> {
            List<LunchUser> notRespondedUsers = userOrderService.findNotRespondedUsers(session);
            telegramNotificationService.sendReminder(session, notRespondedUsers);
            log.info("scheduler_reminder sessionId={} pendingCount={}", session.getId(), notRespondedUsers.size());
        });
    }

    @Scheduled(cron = "${lunch.scheduler.close-cron}", zone = "${lunch.timezone}")
    public void closeDailyLunchOrder() {
        if (!lunchProperties.scheduler().enabled()) {
            return;
        }
        orderSessionService.getTodayOpenSession().ifPresent(session -> {
            OrderSession closed = orderSessionService.closeSession(session.getId(), null);
            SessionSummaryResponse summary = summaryService.buildSummary(closed.getId());
            telegramNotificationService.sendClosedSummary(summary, closed.getId());
            log.info("scheduler_close_session sessionId={}", closed.getId());
        });
    }

    @Scheduled(cron = "${lunch.restaurant-voting.open-cron}", zone = "${lunch.timezone}")
    public void openRestaurantVoting() {
        if (lunchProperties.restaurantVoting() == null || !lunchProperties.restaurantVoting().enabled()) {
            log.info("restaurant_voting_scheduler_skipped reason=disabled");
            return;
        }

        Long groupChatId = lunchProperties.groupChatId();

        log.info("restaurant_voting_scheduler_started groupChatId={}", groupChatId);

        if (groupChatId == null || groupChatId == 0) {
            log.error("restaurant_voting_scheduler_failed reason=group_chat_id_not_configured");
            return;
        }

        try {
            RestaurantVoteSessionResponse response = restaurantVoteSessionService.openSession(null, null);
            if (response == null) {
                log.warn("restaurant_voting_scheduler_completed_without_response groupChatId={}", groupChatId);
                return;
            }

            log.info(
                    "restaurant_voting_scheduler_completed voteSessionId={} groupChatId={} groupMessageId={}",
                    response.id(),
                    groupChatId,
                    response.groupMessageId()
            );
        } catch (BadRequestException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().contains("An open restaurant vote session already exists")) {
                log.warn(
                        "restaurant_voting_scheduler_skipped reason=already_exists groupChatId={} message={}",
                        groupChatId,
                        exception.getMessage()
                );
                return;
            }

            log.error(
                    "restaurant_voting_scheduler_failed groupChatId={} reason={}",
                    groupChatId,
                    exception.getMessage(),
                    exception
            );
        } catch (Exception exception) {
            log.error(
                    "restaurant_voting_scheduler_failed groupChatId={} reason={}",
                    groupChatId,
                    exception.getMessage(),
                    exception
            );
        }
    }

    @Scheduled(cron = "${lunch.restaurant-voting.close-cron}", zone = "${lunch.timezone}")
    public void closeRestaurantVoting() {
        if (lunchProperties.restaurantVoting() == null || !lunchProperties.restaurantVoting().enabled()) {
            return;
        }
        restaurantVoteSessionService.getTodaySession().ifPresent(session -> {
            if (session.getStatus() == uz.company.lunchbot.enums.RestaurantVoteSessionStatus.OPEN) {
                restaurantVoteSessionService.closeSession(session.getId(), null);
            }
        });
    }
}
