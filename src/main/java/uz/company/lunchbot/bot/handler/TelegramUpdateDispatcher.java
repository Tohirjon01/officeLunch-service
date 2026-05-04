package uz.company.lunchbot.bot.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class TelegramUpdateDispatcher {

    private final TelegramMessageHandler telegramMessageHandler;
    private final TelegramCallbackHandler telegramCallbackHandler;

    public void handle(Update update) {
        if (update.hasMessage()) {
            telegramMessageHandler.handle(update.getMessage());
            return;
        }
        if (update.hasCallbackQuery()) {
            telegramCallbackHandler.handle(update.getCallbackQuery());
        }
    }
}
