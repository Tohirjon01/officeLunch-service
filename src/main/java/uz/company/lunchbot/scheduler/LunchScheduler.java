package uz.company.lunchbot.scheduler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.UserOrderService;
import uz.company.lunchbot.service.notification.TelegramNotificationService;
import uz.company.lunchbot.service.report.SummaryService;

@Slf4j
@Component
@RequiredArgsConstructor
public class LunchScheduler {

    private final LunchProperties lunchProperties;
    private final OrderSessionService orderSessionService;
    private final UserOrderService userOrderService;
    private final SummaryService summaryService;
    private final TelegramNotificationService telegramNotificationService;

    @Scheduled(cron = "${lunch.scheduler.open-cron}", zone = "${lunch.timezone}")
    public void openDailyLunchOrder() {
        if (!lunchProperties.scheduler().enabled()) {
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
}
