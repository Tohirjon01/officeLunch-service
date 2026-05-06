package uz.company.lunchbot.service.impl;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.service.TelegramRegistrationStateService;

@Service
public class TelegramRegistrationStateServiceImpl implements TelegramRegistrationStateService {

    private final ConcurrentHashMap<Long, UserLanguage> languages = new ConcurrentHashMap<>();

    @Override
    public void rememberLanguage(Long telegramUserId, UserLanguage language) {
        if (telegramUserId != null && language != null) {
            languages.put(telegramUserId, language);
        }
    }

    @Override
    public Optional<UserLanguage> getLanguage(Long telegramUserId) {
        return Optional.ofNullable(languages.get(telegramUserId));
    }

    @Override
    public void clear(Long telegramUserId) {
        if (telegramUserId != null) {
            languages.remove(telegramUserId);
        }
    }
}
