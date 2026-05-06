package uz.company.lunchbot.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record UpdateRestaurantRequest(
        @NotBlank String name,
        String description,
        @JsonAlias("phone") String phoneNumber,
        String address
) {
}
