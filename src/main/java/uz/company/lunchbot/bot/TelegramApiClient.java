package uz.company.lunchbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uz.company.lunchbot.config.TelegramBotProperties;
import uz.company.lunchbot.exception.TelegramMessageException;

@Slf4j
@Component
public class TelegramApiClient {

    private final TelegramBotProperties telegramBotProperties;
    private final RestClient restClient;

    public TelegramApiClient(TelegramBotProperties telegramBotProperties) {
        this.telegramBotProperties = telegramBotProperties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    public void sendMessage(Long chatId, String text, Object replyMarkup) {
        post("/bot{token}/sendMessage", new TelegramSendMessageRequest(chatId, text, replyMarkup, false));
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        post("/bot{token}/answerCallbackQuery", new TelegramAnswerCallbackRequest(callbackQueryId, text, false));
    }

    private void post(String uri, Object body) {
        try {
            restClient.post()
                    .uri(uri, telegramBotProperties.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.error("telegram_send_failed endpoint={} reason={}", uri, exception.getMessage(), exception);
            throw new TelegramMessageException("Failed to send Telegram message", exception);
        }
    }
}
