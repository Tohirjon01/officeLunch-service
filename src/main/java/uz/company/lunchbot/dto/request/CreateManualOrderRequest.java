package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateManualOrderRequest(
        @NotNull Long userId,
        @NotNull Long menuItemId,
        @Min(1) Integer quantity
) {
}
