package uz.company.lunchbot.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.dto.request.CreateRestaurantRequest;
import uz.company.lunchbot.dto.request.RestaurantContainerSettingsRequest;
import uz.company.lunchbot.dto.request.RestaurantDeliverySettingsRequest;
import uz.company.lunchbot.dto.request.UpdateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.RestaurantRepository;
import uz.company.lunchbot.security.AdminAccessService;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final AdminAccessService adminAccessService;
    private final AuditService auditService;

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
        restaurant.setName(normalizeName(request.name()));
        restaurant.setPhone(blankToNull(request.phone()));
        restaurant.setAddress(blankToNull(request.address()));
        restaurant.setActive(request.active() == null || request.active());
        restaurant.setDefault(Boolean.TRUE.equals(request.defaultRestaurant()));
        restaurant.setContainerEnabled(Boolean.TRUE.equals(request.containerEnabled()));
        restaurant.setDefaultContainerPrice(defaultMoney(request.defaultContainerPrice()));
        restaurant.setDeliveryEnabled(Boolean.TRUE.equals(request.deliveryEnabled()));
        restaurant.setDefaultDeliveryPrice(defaultMoney(request.defaultDeliveryPrice()));

        validateRestaurant(restaurant);
        normalizeDefaultRestaurant(restaurant);

        Restaurant saved = restaurantRepository.save(restaurant);
        auditService.log(AuditAction.RESTAURANT_CREATED, null, actorUserId, null, restaurantState(saved));
        if (saved.isDefault()) {
            auditService.log(AuditAction.RESTAURANT_DEFAULT_CHANGED, null, actorUserId, null, restaurantState(saved));
        }
        return saved;
    }

    @Transactional
    public Restaurant update(Long id, UpdateRestaurantRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant restaurant = getRequired(id);
        Map<String, Object> oldState = restaurantState(restaurant);
        restaurant.setName(normalizeName(request.name()));
        restaurant.setPhone(blankToNull(request.phone()));
        restaurant.setAddress(blankToNull(request.address()));

        Restaurant saved = restaurantRepository.save(restaurant);
        auditService.log(AuditAction.RESTAURANT_UPDATED, null, actorUserId, oldState, restaurantState(saved));
        return saved;
    }

    @Transactional
    public Restaurant updateStatus(Long id, UpdateStatusRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant restaurant = getRequired(id);
        boolean wasActive = restaurant.isActive();
        restaurant.setActive(request.active());
        validateRestaurant(restaurant);

        Restaurant saved = restaurantRepository.save(restaurant);
        if (wasActive != saved.isActive()) {
            auditService.log(saved.isActive() ? AuditAction.RESTAURANT_ACTIVATED : AuditAction.RESTAURANT_DEACTIVATED,
                    null, actorUserId, wasActive, saved.isActive());
        }
        return saved;
    }

    @Transactional
    public Restaurant setDefault(Long id, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant restaurant = getRequired(id);
        if (!restaurant.isActive()) {
            throw new BadRequestException("Default restaurant must be active");
        }

        restaurant.setDefault(true);
        normalizeDefaultRestaurant(restaurant);
        Restaurant saved = restaurantRepository.save(restaurant);
        auditService.log(AuditAction.RESTAURANT_DEFAULT_CHANGED, null, actorUserId, null, restaurantState(saved));
        return saved;
    }

    @Transactional
    public Restaurant updateContainerSettings(Long id, RestaurantContainerSettingsRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant restaurant = getRequired(id);
        Map<String, Object> oldState = restaurantState(restaurant);
        restaurant.setContainerEnabled(request.containerEnabled());
        restaurant.setDefaultContainerPrice(defaultMoney(request.defaultContainerPrice()));
        validateRestaurant(restaurant);

        Restaurant saved = restaurantRepository.save(restaurant);
        auditService.log(AuditAction.RESTAURANT_CONTAINER_CHANGED, null, actorUserId, oldState, restaurantState(saved));
        return saved;
    }

    @Transactional
    public Restaurant updateDeliverySettings(Long id, RestaurantDeliverySettingsRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant restaurant = getRequired(id);
        Map<String, Object> oldState = restaurantState(restaurant);
        restaurant.setDeliveryEnabled(request.deliveryEnabled());
        restaurant.setDefaultDeliveryPrice(defaultMoney(request.defaultDeliveryPrice()));
        validateRestaurant(restaurant);

        Restaurant saved = restaurantRepository.save(restaurant);
        auditService.log(AuditAction.RESTAURANT_DELIVERY_CHANGED, null, actorUserId, oldState, restaurantState(saved));
        return saved;
    }

    private void validateRestaurant(Restaurant restaurant) {
        if (restaurant.getName() == null || restaurant.getName().isBlank()) {
            throw new BadRequestException("Restaurant name must not be blank");
        }
        if (restaurant.isDefault() && !restaurant.isActive()) {
            throw new BadRequestException("Default restaurant must be active");
        }
        if (defaultMoney(restaurant.getDefaultContainerPrice()).signum() < 0) {
            throw new BadRequestException("Default container price must be non-negative");
        }
        if (defaultMoney(restaurant.getDefaultDeliveryPrice()).signum() < 0) {
            throw new BadRequestException("Default delivery price must be non-negative");
        }
    }

    private void normalizeDefaultRestaurant(Restaurant target) {
        if (!target.isDefault()) {
            return;
        }

        Long targetId = target.getId() == null ? -1L : target.getId();
        restaurantRepository.clearDefaultFlagExcept(targetId);
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Restaurant name must not be blank");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, Object> restaurantState(Restaurant restaurant) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("restaurantId", restaurant.getId());
        state.put("name", restaurant.getName());
        state.put("phone", restaurant.getPhone());
        state.put("address", restaurant.getAddress());
        state.put("default", restaurant.isDefault());
        state.put("active", restaurant.isActive());
        state.put("containerEnabled", restaurant.isContainerEnabled());
        state.put("defaultContainerPrice", restaurant.getDefaultContainerPrice());
        state.put("deliveryEnabled", restaurant.isDeliveryEnabled());
        state.put("defaultDeliveryPrice", restaurant.getDefaultDeliveryPrice());
        return state;
    }
}
