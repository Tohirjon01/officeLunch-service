package uz.company.lunchbot.service.calculation;

import java.math.BigDecimal;

public record CalculationResult(
        long orderedCount,
        BigDecimal totalFoodAmount,
        BigDecimal totalContainerAmount,
        BigDecimal deliveryPrice,
        BigDecimal totalFinalAmount,
        BigDecimal roundingDifference
) {
}
