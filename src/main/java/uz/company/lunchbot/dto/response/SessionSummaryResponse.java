package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import uz.company.lunchbot.enums.OrderSessionStatus;

public record SessionSummaryResponse(
        Long sessionId,
        LocalDate orderDate,
        OrderSessionStatus status,
        Long orderedCount,
        Long skippedCount,
        Long noResponseCount,
        List<MealSummaryItemResponse> meals,
        List<UserPaymentResponse> userPayments,
        BigDecimal totalFoodAmount,
        BigDecimal totalContainerAmount,
        BigDecimal deliveryPrice,
        BigDecimal roundedTotal,
        BigDecimal roundingDifference,
        String groupSummaryText,
        String restaurantOrderText
) {
}
