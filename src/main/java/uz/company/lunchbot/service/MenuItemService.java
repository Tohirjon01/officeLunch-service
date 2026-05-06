package uz.company.lunchbot.service;

import java.util.List;
import uz.company.lunchbot.dto.request.CreateMenuItemRequest;
import uz.company.lunchbot.dto.request.MenuItemContainerSettingsRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemCategoryRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemImageRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.entity.MenuItem;
public interface MenuItemService {

    List<MenuItem> getAll(Long restaurantId);

    List<MenuItem> getAllByRestaurant(Long restaurantId);

    MenuItem getRequired(Long id);

    List<MenuItem> getActiveMenu(Long restaurantId);

    List<MenuItem> getDefaultActiveMenu();

    MenuItem create(CreateMenuItemRequest request, Long actorUserId);

    MenuItem update(Long id, UpdateMenuItemRequest request, Long actorUserId);

    MenuItem updateCategory(Long id, UpdateMenuItemCategoryRequest request, Long actorUserId);

    MenuItem updateImage(Long id, UpdateMenuItemImageRequest request, Long actorUserId);

    MenuItem updateStatus(Long id, UpdateStatusRequest request, Long actorUserId);

    MenuItem updateContainerSettings(Long id, MenuItemContainerSettingsRequest request, Long actorUserId);

    MenuItem removeContainerOverride(Long id, Long actorUserId);
}
