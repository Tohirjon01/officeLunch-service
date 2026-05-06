package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChooseRestaurantWinnerRequest(
        @NotNull Long restaurantId
) {
}
