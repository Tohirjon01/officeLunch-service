package uz.company.lunchbot.config;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import uz.company.lunchbot.enums.RoundingStrategy;

@ConfigurationProperties(prefix = "lunch")
public record LunchProperties(
        Long groupChatId,
        String timezone,
        LocalTime defaultDeadlineTime,
        RoundingStrategy roundingStrategy,
        Scheduler scheduler,
        RestaurantVoting restaurantVoting,
        Payment payment,
        Bootstrap bootstrap
) {

    public record Scheduler(
            boolean enabled,
            String openCron,
            String reminderCron,
            String closeCron
    ) {
    }

    public record RestaurantVoting(
            boolean enabled,
            String openCron,
            String closeCron,
            int durationMinutes
    ) {
    }

    public record Payment(
            boolean enabled,
            String cardNumber,
            String cardOwnerName,
            boolean sendReportOnClose
    ) {
    }

    public record Bootstrap(
            Long superAdminTelegramUserId,
            String superAdminUsername,
            String superAdminFirstName,
            String superAdminLastName,
            Long superAdminPrivateChatId
    ) {
    }
}
