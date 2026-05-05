package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantResponse(
        Long id,
        String name,
        String phone,
        String address,
        boolean defaultRestaurant,
        boolean active,
        boolean containerEnabled,
        BigDecimal defaultContainerPrice,
        boolean deliveryEnabled,
        BigDecimal defaultDeliveryPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
