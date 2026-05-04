package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import uz.company.lunchbot.enums.PaymentStatus;
import uz.company.lunchbot.enums.UserOrderStatus;

public record UserOrderResponse(
        Long id,
        Long orderSessionId,
        Long userId,
        String userDisplayName,
        Long menuItemId,
        String menuItemName,
        UserOrderStatus status,
        Integer quantity,
        BigDecimal foodPrice,
        BigDecimal containerPrice,
        BigDecimal deliveryShare,
        BigDecimal finalPrice,
        PaymentStatus paymentStatus,
        LocalDateTime orderedAt,
        LocalDateTime updatedAt
) {
}
