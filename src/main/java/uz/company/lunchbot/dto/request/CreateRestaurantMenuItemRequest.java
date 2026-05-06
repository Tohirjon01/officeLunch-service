package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateRestaurantMenuItemRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        Boolean active,
        @Min(0) Integer sortOrder,
        Boolean containerRequired,
        @DecimalMin(value = "0.00") BigDecimal containerPriceOverride,
        @Size(max = 128) String category,
        @Size(max = 1000) String imageUrl
) {
}
