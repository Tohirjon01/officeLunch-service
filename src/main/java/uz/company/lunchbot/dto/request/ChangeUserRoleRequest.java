package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.NotNull;
import uz.company.lunchbot.enums.UserRole;

public record ChangeUserRoleRequest(@NotNull UserRole role) {
}
