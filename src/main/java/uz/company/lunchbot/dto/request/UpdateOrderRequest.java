package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import uz.company.lunchbot.enums.UserOrderStatus;

public record UpdateOrderRequest(
        Long menuItemId,
        @Min(1) Integer quantity,
        @NotNull UserOrderStatus status
) {
}
