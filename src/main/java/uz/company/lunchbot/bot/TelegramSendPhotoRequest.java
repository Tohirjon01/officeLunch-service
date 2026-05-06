package uz.company.lunchbot.bot;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramSendPhotoRequest(
        Object chat_id,
        String photo,
        String caption,
        Object reply_markup,
        Boolean disable_notification
) {
}
