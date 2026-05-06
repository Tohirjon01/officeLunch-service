package uz.company.lunchbot.service;

import java.util.List;
import java.util.Optional;
import uz.company.lunchbot.dto.request.TelegramRegistrationRequest;
import uz.company.lunchbot.dto.response.RegistrationResultResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserRole;
public interface UserService {

    List<LunchUser> getAll();

    List<LunchUser> getPendingUsers();

    List<LunchUser> getApprovedUsers();

    List<LunchUser> getApprovedAdmins();

    LunchUser getRequired(Long id);

    LunchUser getRequiredByTelegramUserId(Long telegramUserId);

    Optional<LunchUser> findByTelegramUserId(Long telegramUserId);

    Optional<LunchUser> findByPrivateChatId(Long privateChatId);

    LunchUser getApprovedUserByTelegramUserId(Long telegramUserId);

    RegistrationResultResponse registerTelegramUser(TelegramRegistrationRequest request);

    LunchUser completeTelegramRegistration(TelegramRegistrationRequest request, UserLanguage language);

    LunchUser updateLanguage(Long telegramUserId, UserLanguage language);

    LunchUser approve(Long userId, Long actorUserId);

    LunchUser reject(Long userId, Long actorUserId);

    LunchUser block(Long userId, Long actorUserId);

    LunchUser changeRole(Long userId, UserRole role, Long actorUserId);

    LunchUser upsertSuperAdmin(Long telegramUserId, String username, String firstName, String lastName, Long privateChatId);

    void markBotBlocked(Long privateChatId);
}
