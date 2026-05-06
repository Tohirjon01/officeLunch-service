package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantResponse(
        Long id,
        String name,
        String description,
        String phoneNumber,
        String address,
        boolean defaultRestaurant,
        boolean active,
        boolean deliveryEnabled,
        BigDecimal defaultDeliveryPrice,
        boolean containerEnabled,
        BigDecimal defaultContainerPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
