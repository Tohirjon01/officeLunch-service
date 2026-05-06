package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.company.lunchbot.dto.request.CreateMenuItemRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemCategoryRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemImageRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemRequest;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.repository.MenuItemRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.impl.MenuItemServiceImpl;

class MenuItemServiceTest {

    @Test
    void shouldCreateMenuItemForRestaurant() {
        MenuItemRepository menuItemRepository = mock(MenuItemRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        MenuItemServiceImpl service = new MenuItemServiceImpl(menuItemRepository, restaurantService, adminAccessService, auditService);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);

        when(restaurantService.getRequired(1L)).thenReturn(restaurant);
        when(menuItemRepository.findMaxSortOrderByRestaurantId(1L)).thenReturn(11);
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 12L);
            return item;
        });

        MenuItem item = service.create(new CreateMenuItemRequest(
                1L,
                "Burger",
                "Beef burger",
                new BigDecimal("45000"),
                true,
                null,
                null,
                null,
                "Main dishes",
                null
        ), 7L);

        assertThat(item.getId()).isEqualTo(12L);
        assertThat(item.getRestaurant()).isEqualTo(restaurant);
        assertThat(item.getDescription()).isEqualTo("Beef burger");
        assertThat(item.getSortOrder()).isEqualTo(12);
        assertThat(item.getCategory()).isEqualTo("Main dishes");
        assertThat(item.getImageUrl()).isNull();
    }

    @Test
    void shouldGetMenuItemsByRestaurant() {
        MenuItemRepository menuItemRepository = mock(MenuItemRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        MenuItemServiceImpl service = new MenuItemServiceImpl(menuItemRepository, restaurantService, adminAccessService, auditService);

        MenuItem first = new MenuItem();
        first.setSortOrder(1);
        MenuItem second = new MenuItem();
        second.setSortOrder(2);

        when(menuItemRepository.findAllByRestaurantIdOrderBySortOrderAscNameAsc(5L)).thenReturn(List.of(first, second));

        List<MenuItem> items = service.getAllByRestaurant(5L);

        assertThat(items).containsExactly(first, second);
        verify(restaurantService).getRequired(5L);
    }

    @Test
    void shouldHideInactiveMenuItemsFromUserMenu() {
        MenuItemRepository menuItemRepository = mock(MenuItemRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        MenuItemServiceImpl service = new MenuItemServiceImpl(menuItemRepository, restaurantService, adminAccessService, auditService);

        MenuItem activeItem = new MenuItem();
        activeItem.setId(9L);
        activeItem.setActive(true);

        when(menuItemRepository.findAllByRestaurantIdAndIsActiveTrueOrderBySortOrderAscNameAsc(2L)).thenReturn(List.of(activeItem));

        assertThat(service.getActiveMenu(2L)).containsExactly(activeItem);
    }

    @Test
    void shouldUpdateMenuItemWithImageUrl() {
        MenuItemRepository menuItemRepository = mock(MenuItemRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        MenuItemServiceImpl service = new MenuItemServiceImpl(menuItemRepository, restaurantService, adminAccessService, auditService);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);

        MenuItem existing = new MenuItem();
        existing.setRestaurant(restaurant);
        existing.setName("Burger");
        existing.setPrice(new BigDecimal("45000"));
        existing.setSortOrder(1);
        existing.setImageUrl(null);

        when(menuItemRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem updated = service.update(5L, new UpdateMenuItemRequest(
                "Burger",
                null,
                new BigDecimal("45000"),
                1,
                "Fast food",
                "https://example.com/burger.jpg"
        ), 7L);

        assertThat(updated.getCategory()).isEqualTo("Fast food");
        assertThat(updated.getImageUrl()).isEqualTo("https://example.com/burger.jpg");
        assertThat(updated.getTelegramImageFileId()).isNull();
    }

    @Test
    void shouldRemoveMenuItemImageWhenImageRequestClearsIt() {
        MenuItemRepository menuItemRepository = mock(MenuItemRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        MenuItemServiceImpl service = new MenuItemServiceImpl(menuItemRepository, restaurantService, adminAccessService, auditService);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);

        MenuItem existing = new MenuItem();
        existing.setRestaurant(restaurant);
        existing.setImageUrl("https://example.com/burger.jpg");
        existing.setTelegramImageFileId("telegram-file-id");

        when(menuItemRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem updated = service.updateImage(5L, new UpdateMenuItemImageRequest(null, null), 7L);

        assertThat(updated.getImageUrl()).isNull();
        assertThat(updated.getTelegramImageFileId()).isNull();
    }

    @Test
    void shouldUpdateCategorySeparately() {
        MenuItemRepository menuItemRepository = mock(MenuItemRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        MenuItemServiceImpl service = new MenuItemServiceImpl(menuItemRepository, restaurantService, adminAccessService, auditService);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);

        MenuItem existing = new MenuItem();
        existing.setRestaurant(restaurant);
        existing.setCategory("Old");

        when(menuItemRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem updated = service.updateCategory(5L, new UpdateMenuItemCategoryRequest("Grill"), 7L);

        assertThat(updated.getCategory()).isEqualTo("Grill");
    }
}
