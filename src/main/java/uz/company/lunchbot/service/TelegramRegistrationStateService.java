package uz.company.lunchbot.service;

import java.util.Optional;
import uz.company.lunchbot.enums.UserLanguage;

public interface TelegramRegistrationStateService {

    void rememberLanguage(Long telegramUserId, UserLanguage language);

    Optional<UserLanguage> getLanguage(Long telegramUserId);

    void clear(Long telegramUserId);
}
