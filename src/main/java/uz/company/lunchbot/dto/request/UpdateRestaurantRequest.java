package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateRestaurantRequest(
        @NotBlank String name,
        String phone,
        String address
) {
}
