package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MenuItemResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        String name,
        BigDecimal price,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
