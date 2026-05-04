package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import uz.company.lunchbot.enums.PaymentStatus;

public record UserPaymentResponse(
        Long orderId,
        String userDisplayName,
        String mealName,
        BigDecimal finalPrice,
        PaymentStatus paymentStatus
) {
}
