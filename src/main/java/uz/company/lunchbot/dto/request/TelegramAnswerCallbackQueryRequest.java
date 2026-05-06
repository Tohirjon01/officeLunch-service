package uz.company.lunchbot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramAnswerCallbackQueryRequest(
        @JsonProperty("callback_query_id")
        String callbackQueryId,

        String text,

        @JsonProperty("show_alert")
        boolean showAlert
) {
}