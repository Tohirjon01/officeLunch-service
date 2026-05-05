package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import uz.company.lunchbot.enums.RecalculationMode;

public record SessionRecalculationResponse(
        Long sessionId,
        RecalculationMode mode,
        long orderedCount,
        BigDecimal totalFoodAmount,
        BigDecimal totalContainerAmount,
        BigDecimal deliveryPrice,
        BigDecimal totalFinalAmount,
        BigDecimal roundingDifference,
        LocalDateTime recalculatedAt
) {
}
