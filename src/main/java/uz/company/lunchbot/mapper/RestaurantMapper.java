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
                restaurant.getDescription(),
                restaurant.getPhoneNumber(),
                restaurant.getAddress(),
                restaurant.isDefault(),
                restaurant.isActive(),
                restaurant.isDeliveryEnabled(),
                restaurant.getDefaultDeliveryPrice(),
                restaurant.isContainerEnabled(),
                restaurant.getDefaultContainerPrice(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt());
    }
}
