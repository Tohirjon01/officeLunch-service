package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import uz.company.lunchbot.enums.OrderSessionStatus;

public record OrderSessionResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        LocalDate orderDate,
        OrderSessionStatus status,
        BigDecimal deliveryPrice,
        LocalDateTime openedAt,
        LocalDateTime deadlineAt,
        LocalDateTime closedAt,
        LocalDateTime confirmedAt,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
