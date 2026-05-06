package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MenuItemResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        String name,
        String description,
        BigDecimal price,
        boolean active,
        Integer sortOrder,
        Boolean containerRequired,
        BigDecimal containerPriceOverride,
        String category,
        String imageUrl,
        String telegramImageFileId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
