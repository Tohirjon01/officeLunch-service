package uz.company.lunchbot.service.impl;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.service.MenuPhotoUploadStateService;

@Service
public class MenuPhotoUploadStateServiceImpl implements MenuPhotoUploadStateService {

    private final ConcurrentHashMap<Long, Long> pendingUploads = new ConcurrentHashMap<>();

    @Override
    public void remember(Long telegramUserId, Long menuItemId) {
        if (telegramUserId != null && menuItemId != null) {
            pendingUploads.put(telegramUserId, menuItemId);
        }
    }

    @Override
    public Optional<Long> getPendingMenuItemId(Long telegramUserId) {
        return Optional.ofNullable(pendingUploads.get(telegramUserId));
    }

    @Override
    public void clear(Long telegramUserId) {
        if (telegramUserId != null) {
            pendingUploads.remove(telegramUserId);
        }
    }
}
