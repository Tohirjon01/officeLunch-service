package uz.company.lunchbot.service;

import java.util.List;
import uz.company.lunchbot.dto.request.CreateRestaurantRequest;
import uz.company.lunchbot.dto.request.RestaurantContainerSettingsRequest;
import uz.company.lunchbot.dto.request.RestaurantDeliverySettingsRequest;
import uz.company.lunchbot.dto.request.UpdateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.entity.Restaurant;
public interface RestaurantService {

    List<Restaurant> getAll();

    Restaurant getRequired(Long id);

    Restaurant getDefaultActiveRestaurant();

    Restaurant create(CreateRestaurantRequest request, Long actorUserId);

    Restaurant update(Long id, UpdateRestaurantRequest request, Long actorUserId);

    Restaurant updateStatus(Long id, UpdateStatusRequest request, Long actorUserId);

    Restaurant setDefault(Long id, Long actorUserId);

    Restaurant updateContainerSettings(Long id, RestaurantContainerSettingsRequest request, Long actorUserId);

    Restaurant updateDeliverySettings(Long id, RestaurantDeliverySettingsRequest request, Long actorUserId);
}
