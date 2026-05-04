package uz.company.lunchbot.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.dto.request.CreateMenuItemRequest;
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
            return menuItemRepository.findAll();
        }
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
        Restaurant restaurant = request.restaurantId() == null
                ? restaurantService.getDefaultActiveRestaurant()
                : restaurantService.getRequired(request.restaurantId());
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        menuItem.setName(request.name().trim());
        menuItem.setPrice(request.price());
        menuItem.setActive(request.active() == null || request.active());
        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_CREATED, null, actorUserId, null, saved.getId());
        return saved;
    }

    @Transactional
    public MenuItem update(Long id, UpdateMenuItemRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        MenuItem menuItem = getRequired(id);
        String oldValue = menuItem.getName() + ":" + menuItem.getPrice();
        menuItem.setName(request.name().trim());
        menuItem.setPrice(request.price());
        menuItem.setActive(request.active());
        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(AuditAction.MENU_ITEM_UPDATED, null, actorUserId, oldValue, saved.getName() + ":" + saved.getPrice());
        return saved;
    }

    @Transactional
    public MenuItem updateStatus(Long id, UpdateStatusRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        if (request.defaultRestaurant() != null) {
            throw new BadRequestException("Menu item status request does not support defaultRestaurant");
        }
        MenuItem menuItem = getRequired(id);
        menuItem.setActive(request.active());
        MenuItem saved = menuItemRepository.save(menuItem);
        auditService.log(request.active() ? AuditAction.MENU_ITEM_ACTIVATED : AuditAction.MENU_ITEM_DEACTIVATED,
                null, actorUserId, null, saved.getId());
        return saved;
    }
}
