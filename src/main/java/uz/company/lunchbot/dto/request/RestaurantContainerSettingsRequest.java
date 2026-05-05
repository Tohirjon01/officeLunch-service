package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RestaurantContainerSettingsRequest(
        @NotNull Boolean containerEnabled,
        @NotNull @DecimalMin(value = "0.00") BigDecimal defaultContainerPrice
) {
}
