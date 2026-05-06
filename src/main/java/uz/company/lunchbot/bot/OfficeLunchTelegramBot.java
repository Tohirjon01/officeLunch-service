package uz.company.lunchbot.bot;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.company.lunchbot.bot.handler.TelegramCallbackHandler;
import uz.company.lunchbot.bot.handler.TelegramMessageHandler;
import uz.company.lunchbot.config.TelegramBotProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfficeLunchTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotProperties telegramProperties;
    private final TelegramMessageHandler messageHandler;
    private final TelegramCallbackHandler callbackHandler;

    @PostConstruct
    public void init() {
        log.info("telegram_bot_username={}", telegramProperties.username());
        log.info(
                "telegram_bot_token_configured={}",
                telegramProperties.token() != null && !telegramProperties.token().isBlank()
        );
        log.info(
                "telegram_bot_token_length={}",
                telegramProperties.token() == null ? 0 : telegramProperties.token().length()
        );
    }

    @Override
    public String getBotUsername() {
        return telegramProperties.username() == null ? null : telegramProperties.username().trim();
    }

    @Override
    public String getBotToken() {
        return telegramProperties.token() == null ? null : telegramProperties.token().trim();
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("telegram_update_received updateId={}", update.getUpdateId());

        if (update.hasMessage()) {
            var message = update.getMessage();

            log.info(
                    "telegram_message_received chatId={} userId={} hasText={} hasContact={} hasPhoto={} hasDocument={}",
                    message.getChatId(),
                    message.getFrom().getId(),
                    message.hasText(),
                    message.hasContact(),
                    message.hasPhoto(),
                    message.hasDocument()
            );

            messageHandler.handle(message);
            return;
        }

        if (update.hasCallbackQuery()) {
            log.info(
                    "telegram_callback_received userId={} data={}",
                    update.getCallbackQuery().getFrom().getId(),
                    update.getCallbackQuery().getData()
            );

            callbackHandler.handle(update.getCallbackQuery());
        }
    }
}
