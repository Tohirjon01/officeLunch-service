package uz.company.lunchbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.company.lunchbot.bot.handler.TelegramUpdateDispatcher;
import uz.company.lunchbot.config.TelegramBotProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfficeLunchTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotProperties telegramBotProperties;
    private final TelegramUpdateDispatcher telegramUpdateDispatcher;

    @Override
    public String getBotUsername() {
        return telegramBotProperties.username();
    }

    @Override
    public String getBotToken() {
        return telegramBotProperties.token();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            telegramUpdateDispatcher.handle(update);
        } catch (Exception exception) {
            log.error("telegram_update_handling_failed reason={}", exception.getMessage(), exception);
        }
    }
}
