package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateMenuItemRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        @Min(0) Integer sortOrder,
        @Size(max = 128) String category,
        @Size(max = 1000) String imageUrl
) {
}
