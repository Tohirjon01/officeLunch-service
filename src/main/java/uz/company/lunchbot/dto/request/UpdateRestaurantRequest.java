package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateRestaurantRequest(
        @NotBlank String name,
        String phone,
        String address,
        @NotNull Boolean defaultRestaurant,
        @NotNull Boolean active
) {
}
