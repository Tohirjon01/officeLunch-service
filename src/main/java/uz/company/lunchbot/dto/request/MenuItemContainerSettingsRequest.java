package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record MenuItemContainerSettingsRequest(
        Boolean containerRequired,
        @DecimalMin(value = "0.00") BigDecimal containerPriceOverride
) {
}
