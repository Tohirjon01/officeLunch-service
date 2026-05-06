package uz.company.lunchbot.mapper;

import org.springframework.stereotype.Component;
import uz.company.lunchbot.dto.response.MenuItemResponse;
import uz.company.lunchbot.entity.MenuItem;

@Component
public class MenuItemMapper {

    public MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getRestaurant().getId(),
                item.getRestaurant().getName(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isActive(),
                item.getSortOrder(),
                item.getContainerRequired(),
                item.getContainerPriceOverride(),
                item.getCategory(),
                item.getImageUrl(),
                item.getTelegramImageFileId(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
