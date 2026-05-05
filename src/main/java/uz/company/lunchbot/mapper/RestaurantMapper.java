package uz.company.lunchbot.mapper;

import org.springframework.stereotype.Component;
import uz.company.lunchbot.dto.response.RestaurantResponse;
import uz.company.lunchbot.entity.Restaurant;

@Component
public class RestaurantMapper {

    public RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getPhone(),
                restaurant.getAddress(),
                restaurant.isDefault(),
                restaurant.isActive(),
                restaurant.isContainerEnabled(),
                restaurant.getDefaultContainerPrice(),
                restaurant.isDeliveryEnabled(),
                restaurant.getDefaultDeliveryPrice(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt());
    }
}
