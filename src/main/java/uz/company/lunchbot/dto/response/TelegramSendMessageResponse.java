package uz.company.lunchbot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramSendMessageResponse(
        boolean ok,
        TelegramMessageResult result
) {
    public record TelegramMessageResult(
            @JsonProperty("message_id")
            Long messageId
    ) {
    }
}