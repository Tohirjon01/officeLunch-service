package uz.company.lunchbot.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.dto.request.CreateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.RestaurantRepository;
import uz.company.lunchbot.security.AdminAccessService;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final AdminAccessService adminAccessService;

    public List<Restaurant> getAll() {
        return restaurantRepository.findAllByOrderByCreatedAtDesc();
    }

    public Restaurant getRequired(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Restaurant not found: " + id));
    }

    public Restaurant getDefaultActiveRestaurant() {
        return restaurantRepository.findByIsDefaultTrueAndIsActiveTrue()
                .orElseThrow(() -> new NotFoundException("Default active restaurant not found"));
    }

    @Transactional
    public Restaurant create(CreateRestaurantRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.name().trim());
        restaurant.setPhone(request.phone());
        restaurant.setAddress(request.address());
        restaurant.setActive(request.active() == null || request.active());
        restaurant.setDefault(Boolean.TRUE.equals(request.defaultRestaurant()));
        validateDefaultRestaurant(restaurant.isDefault(), restaurant.isActive());
        normalizeDefaultRestaurant(restaurant, restaurant.isDefault());
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant update(Long id, UpdateRestaurantRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        Restaurant restaurant = getRequired(id);
        restaurant.setName(request.name().trim());
        restaurant.setPhone(request.phone());
        restaurant.setAddress(request.address());
        restaurant.setActive(request.active());
        restaurant.setDefault(request.defaultRestaurant());
        validateDefaultRestaurant(restaurant.isDefault(), restaurant.isActive());
        normalizeDefaultRestaurant(restaurant, restaurant.isDefault());
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant updateStatus(Long id, UpdateStatusRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        Restaurant restaurant = getRequired(id);
        restaurant.setActive(request.active());
        if (request.defaultRestaurant() != null) {
            restaurant.setDefault(request.defaultRestaurant());
        }
        validateDefaultRestaurant(restaurant.isDefault(), restaurant.isActive());
        normalizeDefaultRestaurant(restaurant, restaurant.isDefault());
        return restaurantRepository.save(restaurant);
    }

    private void validateDefaultRestaurant(boolean defaultRestaurant, boolean active) {
        if (defaultRestaurant && !active) {
            throw new BadRequestException("Default restaurant must be active");
        }
    }

    private void normalizeDefaultRestaurant(Restaurant target, boolean defaultRestaurant) {
        if (!defaultRestaurant) {
            return;
        }
        restaurantRepository.findAll().stream()
                .filter(existing -> existing.isDefault() && !existing.getId().equals(target.getId()))
                .forEach(existing -> {
                    existing.setDefault(false);
                    restaurantRepository.save(existing);
                });
    }
}
