package uz.company.lunchbot.dto.request;

public record TelegramRegistrationRequest(
        Long telegramUserId,
        String username,
        String firstName,
        String lastName,
        Long privateChatId,
        String phoneNumber
) {
}
