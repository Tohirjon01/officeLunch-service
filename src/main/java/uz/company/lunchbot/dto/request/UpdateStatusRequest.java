package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull Boolean active,
        Boolean defaultRestaurant
) {
}
