package uz.company.lunchbot.mapper;

import org.springframework.stereotype.Component;
import uz.company.lunchbot.dto.response.UserResponse;
import uz.company.lunchbot.entity.LunchUser;

@Component
public class UserMapper {

    public UserResponse toResponse(LunchUser user) {
        return new UserResponse(
                user.getId(),
                user.getTelegramUserId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getPrivateChatId(),
                user.getStatus(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
