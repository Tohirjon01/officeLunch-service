package uz.company.lunchbot.bot;

public record TelegramAnswerCallbackRequest(
        String callback_query_id,
        String text,
        Boolean show_alert
) {
}
