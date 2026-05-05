package uz.company.lunchbot.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.dto.request.CreateMenuItemRequest;
import uz.company.lunchbot.dto.request.MenuItemContainerSettingsRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.MenuItemRepository;
import uz.company.lunchbot.security.AdminAccessService;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;
    private final AdminAccessService adminAccessService;
    private final AuditService auditService;

    public List<MenuItem> getAll(Long restaurantId) {
        if (restaurantId == null) {
            return menuItemRepository.findAllByOrderByCreatedAtDesc();
        }
        restaurantService.getRequired(restaurantId);
        return menuItemRepository.findAllByRestaurantIdOrderByNameAsc(restaurantId);
    }

    public List<MenuItem> getAllByRestaurant(Long restaurantId) {
        restaurantService.getRequired(restaurantId);
        return menuItemRepository.findAllByRestaurantIdOrderByNameAsc(restaurantId);
    }

    public MenuItem getRequired(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu item not found: " + id));
    }

    public List<MenuItem> getActiveMenu(Long restaurantId) {
        return menuItemRepository.findAllByRestaurantIdAndIsActiveTrueOrderByNameAsc(restaurantId);
    }

    public List<MenuItem> getDefaultActiveMenu() {
        Restaurant restaurant = restaurantService.getDefaultActiveRestaurant();
        return getActiveMenu(restaurant.getId());
    }

    @Transactional
    public MenuItem create(CreateMenuItemRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant restaurant = restaurantService.getRequired(request.restaurantId());
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        menuItem.setName(normalizeName(request.name()));
        menuItem.setPrice(requireNonNegative(request.price(), "Menu item price must be non-negative"));
        menuItem.setActive(request.active() == null || request.active());
        applyContainerSettings(menuItem, request.containerRequired(), request.containerPriceOverride());

        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_CREATED, null, actorUserId, null, menuItemState(saved));
        return saved;
    }

    @Transactional
    public MenuItem update(Long id, UpdateMenuItemRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        MenuItem menuItem = getRequired(id);
        Map<String, Object> oldState = menuItemState(menuItem);
        BigDecimal oldPrice = menuItem.getPrice();
        menuItem.setName(normalizeName(request.name()));
        menuItem.setPrice(requireNonNegative(request.price(), "Menu item price must be non-negative"));

        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_UPDATED, null, actorUserId, oldState, menuItemState(saved));
        if (oldPrice.compareTo(saved.getPrice()) != 0) {
            auditService.log(AuditAction.MENU_ITEM_PRICE_CHANGED, null, actorUserId, oldPrice, saved.getPrice());
        }
        return saved;
    }

    @Transactional
    public MenuItem updateStatus(Long id, UpdateStatusRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        MenuItem menuItem = getRequired(id);
        boolean wasActive = menuItem.isActive();
        menuItem.setActive(request.active());
        MenuItem saved = menuItemRepository.save(menuItem);
        if (wasActive != saved.isActive()) {
            auditService.log(saved.isActive() ? AuditAction.MENU_ITEM_ACTIVATED : AuditAction.MENU_ITEM_DEACTIVATED,
                    null, actorUserId, wasActive, saved.isActive());
        }
        return saved;
    }

    @Transactional
    public MenuItem updateContainerSettings(Long id, MenuItemContainerSettingsRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        MenuItem menuItem = getRequired(id);
        Map<String, Object> oldState = menuItemState(menuItem);
        applyContainerSettings(menuItem, request.containerRequired(), request.containerPriceOverride());

        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_CONTAINER_CHANGED, null, actorUserId, oldState, menuItemState(saved));
        return saved;
    }

    @Transactional
    public MenuItem removeContainerOverride(Long id, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        MenuItem menuItem = getRequired(id);
        Map<String, Object> oldState = menuItemState(menuItem);
        menuItem.setContainerPriceOverride(null);

        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_CONTAINER_CHANGED, null, actorUserId, oldState, menuItemState(saved));
        return saved;
    }

    private void applyContainerSettings(MenuItem menuItem, Boolean containerRequired, BigDecimal containerPriceOverride) {
        BigDecimal normalizedOverride = containerPriceOverride == null
                ? null
                : requireNonNegative(containerPriceOverride, "Container price override must be non-negative");

        menuItem.setContainerRequired(containerRequired);
        menuItem.setContainerPriceOverride(Boolean.FALSE.equals(containerRequired) ? null : normalizedOverride);
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Menu item name must not be blank");
        }
        return value.trim();
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private Map<String, Object> menuItemState(MenuItem menuItem) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("menuItemId", menuItem.getId());
        state.put("restaurantId", menuItem.getRestaurant().getId());
        state.put("name", menuItem.getName());
        state.put("price", menuItem.getPrice());
        state.put("active", menuItem.isActive());
        state.put("containerRequired", menuItem.getContainerRequired());
        state.put("containerPriceOverride", menuItem.getContainerPriceOverride());
        return state;
    }
}
