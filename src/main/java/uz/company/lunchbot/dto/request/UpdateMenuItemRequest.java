package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateMenuItemRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        @NotNull Boolean active
) {
}
