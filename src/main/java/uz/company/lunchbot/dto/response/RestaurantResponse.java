package uz.company.lunchbot.dto.response;

import java.time.LocalDateTime;

public record RestaurantResponse(
        Long id,
        String name,
        String phone,
        String address,
        boolean defaultRestaurant,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
