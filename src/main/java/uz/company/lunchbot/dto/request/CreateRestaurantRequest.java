package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRestaurantRequest(
        @NotBlank String name,
        String phone,
        String address,
        Boolean defaultRestaurant,
        Boolean active
) {
}
