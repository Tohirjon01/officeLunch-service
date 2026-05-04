package uz.company.lunchbot.bot;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramSendMessageRequest(
        Object chat_id,
        String text,
        Object reply_markup,
        Boolean disable_notification
) {
}
