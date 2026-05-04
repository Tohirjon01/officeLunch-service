package uz.company.lunchbot.dto.response;

import java.time.LocalDateTime;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;

public record UserResponse(
        Long id,
        Long telegramUserId,
        String username,
        String firstName,
        String lastName,
        String displayName,
        Long privateChatId,
        UserStatus status,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
