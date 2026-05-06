package uz.company.lunchbot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import uz.company.lunchbot.bot.OfficeLunchTelegramBot;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramBotRegistrar implements ApplicationRunner {

    private final OfficeLunchTelegramBot officeLunchTelegramBot;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(officeLunchTelegramBot);

            log.info("telegram_bot_registered username={}", officeLunchTelegramBot.getBotUsername());
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Telegram bot registration failed. Verify BOT_TOKEN or set BOT_ENABLED=false to start without Telegram bot registration.",
                    exception
            );
        }
    }
}
