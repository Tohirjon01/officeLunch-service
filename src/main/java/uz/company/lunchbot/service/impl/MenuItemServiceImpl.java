package uz.company.lunchbot.service.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.dto.request.CreateMenuItemRequest;
import uz.company.lunchbot.dto.request.MenuItemContainerSettingsRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemCategoryRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemImageRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.MenuItemRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.AuditService;
import uz.company.lunchbot.service.MenuItemService;
import uz.company.lunchbot.service.RestaurantService;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;
    private final AdminAccessService adminAccessService;
    private final AuditService auditService;

    @Override
    public List<MenuItem> getAll(Long restaurantId) {
        if (restaurantId == null) {
            return menuItemRepository.findAllByOrderByCreatedAtDesc();
        }
        restaurantService.getRequired(restaurantId);
        return menuItemRepository.findAllByRestaurantIdOrderBySortOrderAscNameAsc(restaurantId);
    }

    @Override
    public List<MenuItem> getAllByRestaurant(Long restaurantId) {
        restaurantService.getRequired(restaurantId);
        return menuItemRepository.findAllByRestaurantIdOrderBySortOrderAscNameAsc(restaurantId);
    }

    @Override
    public MenuItem getRequired(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu item not found: " + id));
    }

    @Override
    public List<MenuItem> getActiveMenu(Long restaurantId) {
        return menuItemRepository.findAllByRestaurantIdAndIsActiveTrueOrderBySortOrderAscNameAsc(restaurantId);
    }

    @Override
    public List<MenuItem> getDefaultActiveMenu() {
        Restaurant restaurant = restaurantService.getDefaultActiveRestaurant();
        return getActiveMenu(restaurant.getId());
    }

    @Override
    @Transactional
    public MenuItem create(CreateMenuItemRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        Restaurant restaurant = restaurantService.getRequired(request.restaurantId());
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        menuItem.setName(normalizeName(request.name()));
        menuItem.setDescription(blankToNull(request.description()));
        menuItem.setPrice(requirePositive(request.price(), "Menu item price must be positive"));
        menuItem.setActive(request.active() == null || request.active());
        menuItem.setSortOrder(resolveSortOrder(restaurant.getId(), request.sortOrder()));
        applyContainerSettings(menuItem, request.containerRequired(), request.containerPriceOverride());
        menuItem.setCategory(normalizeCategory(request.category()));
        menuItem.setImageUrl(normalizeImageUrl(request.imageUrl()));
        menuItem.setTelegramImageFileId(null);

        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_CREATED, null, actorUserId, null, menuItemState(saved));
        return saved;
    }

    @Override
    @Transactional
    public MenuItem update(Long id, UpdateMenuItemRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        MenuItem menuItem = getRequired(id);
        Map<String, Object> oldState = menuItemState(menuItem);
        BigDecimal oldPrice = menuItem.getPrice();
        menuItem.setName(normalizeName(request.name()));
        menuItem.setDescription(blankToNull(request.description()));
        menuItem.setPrice(requirePositive(request.price(), "Menu item price must be positive"));
        menuItem.setSortOrder(normalizeSortOrder(request.sortOrder(), menuItem.getSortOrder()));
        if (request.category() != null) {
            menuItem.setCategory(normalizeCategory(request.category()));
        }
        if (request.imageUrl() != null) {
            String normalizedImageUrl = normalizeImageUrl(request.imageUrl());
            if (!java.util.Objects.equals(normalizedImageUrl, menuItem.getImageUrl())) {
                menuItem.setTelegramImageFileId(null);
            }
            menuItem.setImageUrl(normalizedImageUrl);
        }

        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_UPDATED, null, actorUserId, oldState, menuItemState(saved));
        if (oldPrice.compareTo(saved.getPrice()) != 0) {
            auditService.log(AuditAction.MENU_ITEM_PRICE_CHANGED, null, actorUserId, oldPrice, saved.getPrice());
        }
        return saved;
    }

    @Override
    @Transactional
    public MenuItem updateCategory(Long id, UpdateMenuItemCategoryRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        MenuItem menuItem = getRequired(id);
        Map<String, Object> oldState = menuItemState(menuItem);
        menuItem.setCategory(normalizeCategory(request.category()));

        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_UPDATED, null, actorUserId, oldState, menuItemState(saved));
        return saved;
    }

    @Override
    @Transactional
    public MenuItem updateImage(Long id, UpdateMenuItemImageRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        MenuItem menuItem = getRequired(id);
        Map<String, Object> oldState = menuItemState(menuItem);
        menuItem.setImageUrl(normalizeImageUrl(request.imageUrl()));
        menuItem.setTelegramImageFileId(normalizeTelegramImageFileId(request.telegramImageFileId()));

        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_UPDATED, null, actorUserId, oldState, menuItemState(saved));
        return saved;
    }

    @Override
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

    @Override
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

    @Override
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeImageUrl(String value) {
        String normalized = blankToNull(value);
        if (normalized != null && normalized.length() > 1000) {
            throw new BadRequestException("Menu item image URL must be at most 1000 characters");
        }
        return normalized;
    }

    private String normalizeCategory(String value) {
        String normalized = blankToNull(value);
        if (normalized != null && normalized.length() > 128) {
            throw new BadRequestException("Menu item category must be at most 128 characters");
        }
        return normalized;
    }

    private String normalizeTelegramImageFileId(String value) {
        String normalized = blankToNull(value);
        if (normalized != null && normalized.length() > 512) {
            throw new BadRequestException("Telegram image file id must be at most 512 characters");
        }
        return normalized;
    }

    private int resolveSortOrder(Long restaurantId, Integer requestedSortOrder) {
        if (requestedSortOrder != null) {
            if (requestedSortOrder < 0) {
                throw new BadRequestException("Menu item sort order must be non-negative");
            }
            return requestedSortOrder;
        }
        return menuItemRepository.findMaxSortOrderByRestaurantId(restaurantId) + 1;
    }

    private int normalizeSortOrder(Integer requestedSortOrder, Integer currentSortOrder) {
        if (requestedSortOrder == null) {
            return currentSortOrder == null ? 0 : currentSortOrder;
        }
        if (requestedSortOrder < 0) {
            throw new BadRequestException("Menu item sort order must be non-negative");
        }
        return requestedSortOrder;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private Map<String, Object> menuItemState(MenuItem menuItem) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("menuItemId", menuItem.getId());
        state.put("restaurantId", menuItem.getRestaurant().getId());
        state.put("name", menuItem.getName());
        state.put("description", menuItem.getDescription());
        state.put("price", menuItem.getPrice());
        state.put("active", menuItem.isActive());
        state.put("sortOrder", menuItem.getSortOrder());
        state.put("containerRequired", menuItem.getContainerRequired());
        state.put("containerPriceOverride", menuItem.getContainerPriceOverride());
        state.put("category", menuItem.getCategory());
        state.put("imageUrl", menuItem.getImageUrl());
        state.put("telegramImageFileId", menuItem.getTelegramImageFileId());
        return state;
    }
}
