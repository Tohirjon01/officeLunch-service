package uz.company.lunchbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uz.company.lunchbot.config.TelegramBotProperties;
import uz.company.lunchbot.dto.request.TelegramAnswerCallbackQueryRequest;
import uz.company.lunchbot.dto.response.TelegramSendMessageResponse;
import uz.company.lunchbot.exception.TelegramMessageException;
import uz.company.lunchbot.service.UserService;
import java.util.Map;

@Slf4j
@Component
public class TelegramApiClient {

    private final TelegramBotProperties telegramBotProperties;
    private final RestClient restClient;
    private final UserService userService;

    @Autowired
    public TelegramApiClient(TelegramBotProperties telegramBotProperties, UserService userService) {
        this(
                telegramBotProperties,
                RestClient.builder()
                        .baseUrl("https://api.telegram.org")
                        .build(),
                userService
        );
    }

    TelegramApiClient(TelegramBotProperties telegramBotProperties, RestClient restClient) {
        this(telegramBotProperties, restClient, null);
    }

    TelegramApiClient(TelegramBotProperties telegramBotProperties, RestClient restClient, UserService userService) {
        this.telegramBotProperties = telegramBotProperties;
        this.restClient = restClient;
        this.userService = userService;
    }

    public Long sendMessage(Long chatId, String text, Object replyMarkup) {
        try {
            TelegramSendMessageRequest request =
                    new TelegramSendMessageRequest(chatId, text, replyMarkup, false);

            TelegramSendMessageResponse response = restClient
                    .post()
                    .uri("/bot{token}/sendMessage", token())
                    .body(request)
                    .retrieve()
                    .body(TelegramSendMessageResponse.class);

            Long messageId = response != null && response.result() != null
                    ? response.result().messageId()
                    : null;

            log.info("telegram_message_sent chatId={} messageId={}", chatId, messageId);

            return messageId;
        } catch (Exception exception) {
            if (isBlockedByUser(exception)) {
                markBotBlocked(chatId);
                log.warn("telegram_send_blocked endpoint=/bot{token}/sendMessage reason={}", blockedReason(exception));
                return null;
            }

            log.error(
                    "telegram_send_failed endpoint=/bot{token}/sendMessage chatId={} reason={}",
                    chatId,
                    exception.getMessage(),
                    exception
            );
            throw new TelegramMessageException("Failed to send Telegram message", exception);
        }
    }

    public boolean sendPhoto(Long chatId, String photo, String caption, Object replyMarkup) {
        if (chatId == null || photo == null || photo.isBlank()) {
            return false;
        }

        try {
            restClient.post()
                    .uri("/bot{token}/sendPhoto", token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TelegramSendPhotoRequest(chatId, photo, caption, replyMarkup, false))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception exception) {
            if (isBlockedByUser(exception)) {
                markBotBlocked(chatId);
                log.warn("telegram_photo_blocked reason={}", blockedReason(exception));
                return false;
            }

            log.warn("telegram_photo_send_failed chatId={} reason={}", chatId, blockedReason(exception));
            return false;
        }
    }

    public Long sendMessageAndGetMessageId(Long chatId, String text, Object replyMarkup) {
        Map<String, Object> response = postForMap("/bot{token}/sendMessage", new TelegramSendMessageRequest(chatId, text, replyMarkup, false), chatId);
        Object result = response.get("result");
        if (!(result instanceof Map<?, ?> resultMap)) {
            return null;
        }
        Object messageId = resultMap.get("message_id");
        if (messageId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            restClient.post()
                    .uri("/bot{token}/answerCallbackQuery", token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TelegramAnswerCallbackQueryRequest(callbackQueryId, text, true))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            if (isExpiredCallbackQuery(exception)) {
                log.warn("telegram_callback_query_expired callbackQueryId={} reason={}", callbackQueryId, blockedReason(exception));
                return;
            }

            throw new TelegramMessageException("Failed to answer Telegram callback query", exception);
        }
    }
    public void editMessageText(Long chatId, Long messageId, String text, Object replyMarkup) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("message_id", messageId);
        body.put("text", text);
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }
        post(
                "/bot{token}/editMessageText",
                body,
                null
        );
    }

    public void copyMessage(Long chatId, Long fromChatId, Long messageId) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("from_chat_id", fromChatId);
        body.put("message_id", messageId);
        post("/bot{token}/copyMessage", body, chatId);
    }

    private void post(String uri, Object body, Long chatId) {
        try {
            restClient.post()
                    .uri(uri, token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            if (isBlockedByUser(exception)) {
                markBotBlocked(chatId);
                log.warn("telegram_send_blocked endpoint={} reason={}", uri, blockedReason(exception));
                return;
            }
            log.error("telegram_send_failed endpoint={} reason={}", uri, exception.getMessage(), exception);
            throw new TelegramMessageException("Failed to send Telegram message", exception);
        }
    }

    private Map<String, Object> postForMap(String uri, Object body, Long chatId) {
        try {
            return restClient.post()
                    .uri(uri, token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception exception) {
            if (isBlockedByUser(exception)) {
                markBotBlocked(chatId);
                log.warn("telegram_send_blocked endpoint={} reason={}", uri, blockedReason(exception));
                return Map.of();
            }
            log.error("telegram_send_failed endpoint={} reason={}", uri, exception.getMessage(), exception);
            throw new TelegramMessageException("Failed to send Telegram message", exception);
        }
    }

    private void markBotBlocked(Long chatId) {
        if (chatId == null || userService == null) {
            return;
        }

        try {
            userService.markBotBlocked(chatId);
        } catch (Exception exception) {
            log.warn("telegram_blocked_user_mark_failed chatId={} reason={}", chatId, exception.getMessage());
        }
    }

    private boolean isBlockedByUser(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value() == 403
                    && containsBlockedMessage(responseException.getResponseBodyAsString());
        }
        return containsBlockedMessage(exception.getMessage());
    }

    private boolean containsBlockedMessage(String value) {
        return value != null && value.toLowerCase().contains("bot was blocked by the user");
    }

    private boolean isExpiredCallbackQuery(Exception exception) {
        String reason = blockedReason(exception);
        if (reason == null) {
            return false;
        }

        String normalized = reason.toLowerCase();
        return normalized.contains("query is too old")
                || normalized.contains("response timeout expired")
                || normalized.contains("query id is invalid");
    }

    private String blockedReason(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                return body;
            }
        }
        return exception.getMessage();
    }

    private String token() {
        return telegramBotProperties.token() == null ? null : telegramBotProperties.token().trim();
    }
}
