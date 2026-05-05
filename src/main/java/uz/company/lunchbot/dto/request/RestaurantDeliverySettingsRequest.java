package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RestaurantDeliverySettingsRequest(
        @NotNull Boolean deliveryEnabled,
        @NotNull @DecimalMin(value = "0.00") BigDecimal defaultDeliveryPrice
) {
}
