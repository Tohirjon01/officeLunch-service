package uz.company.lunchbot.service;

import java.util.Optional;

public interface MenuPhotoUploadStateService {

    void remember(Long telegramUserId, Long menuItemId);

    Optional<Long> getPendingMenuItemId(Long telegramUserId);

    void clear(Long telegramUserId);
}
