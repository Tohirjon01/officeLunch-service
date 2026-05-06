package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.dto.request.UpdateMenuItemCategoryRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemImageRequest;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.MenuPhotoUploadStateService;
import uz.company.lunchbot.service.impl.TelegramAdminCommandServiceImpl;

class TelegramAdminCommandServiceImplTest {

    @Test
    void shouldSupportMenuImageRemovalCommand() {
        RestaurantService restaurantService = mock(RestaurantService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        SummaryService summaryService = mock(SummaryService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramAdminCommandServiceImpl service = new TelegramAdminCommandServiceImpl(
                restaurantService,
                menuItemService,
                orderSessionService,
                restaurantVoteSessionService,
                summaryService,
                userOrderService,
                adminAccessService,
                new TelegramKeyboards(),
                menuPhotoUploadStateService
        );

        LunchUser actor = new LunchUser();
        actor.setId(7L);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(15L);
        menuItem.setName("Bon file");
        menuItem.setRestaurant(restaurant);
        when(menuItemService.updateImage(eq(15L), eq(new UpdateMenuItemImageRequest(null, null)), eq(7L))).thenReturn(menuItem);

        TelegramAdminCommandService.AdminCommandResponse result = service.handle(actor, "/menu_image 15 | --");

        assertThat(result.text()).contains("Menu item image updated");
        assertThat(result.keyboard()).isNull();
        verify(menuItemService).updateImage(15L, new UpdateMenuItemImageRequest(null, null), 7L);
    }

    @Test
    void shouldShowFirstMenuPageWithNextButton() {
        RestaurantService restaurantService = mock(RestaurantService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        SummaryService summaryService = mock(SummaryService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramAdminCommandServiceImpl service = new TelegramAdminCommandServiceImpl(
                restaurantService,
                menuItemService,
                orderSessionService,
                restaurantVoteSessionService,
                summaryService,
                userOrderService,
                adminAccessService,
                new TelegramKeyboards(),
                menuPhotoUploadStateService
        );

        LunchUser actor = new LunchUser();
        actor.setId(7L);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setName("Tarnov");
        restaurant.setContainerEnabled(true);
        when(restaurantService.getRequired(2L)).thenReturn(restaurant);
        when(menuItemService.getAllByRestaurant(2L)).thenReturn(items(restaurant, 12));

        TelegramAdminCommandService.AdminCommandResponse response = service.handle(actor, "/menu 2");

        assertThat(response.text()).contains("Tarnov menu — page 1/2");
        assertThat(menuLineCount(response.text())).isEqualTo(10);
        assertThat(response.text()).contains("📷");
        assertThat(firstCallback(response.keyboard())).isEqualTo("MENU_PAGE:2:1");
    }

    @Test
    void shouldBoundRequestedPageToLastPage() {
        RestaurantService restaurantService = mock(RestaurantService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        SummaryService summaryService = mock(SummaryService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramAdminCommandServiceImpl service = new TelegramAdminCommandServiceImpl(
                restaurantService,
                menuItemService,
                orderSessionService,
                restaurantVoteSessionService,
                summaryService,
                userOrderService,
                adminAccessService,
                new TelegramKeyboards(),
                menuPhotoUploadStateService
        );

        LunchUser actor = new LunchUser();
        actor.setId(7L);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setName("Tarnov");
        restaurant.setContainerEnabled(true);
        when(restaurantService.getRequired(2L)).thenReturn(restaurant);
        when(menuItemService.getAllByRestaurant(2L)).thenReturn(items(restaurant, 12));

        TelegramAdminCommandService.AdminCommandResponse response = service.buildMenuPage(actor, 2L, 999);

        assertThat(response.text()).contains("Tarnov menu — page 2/2");
        assertThat(menuLineCount(response.text())).isEqualTo(2);
        assertThat(firstCallback(response.keyboard())).isEqualTo("MENU_PAGE:2:0");
    }

    @Test
    void shouldStartAndCancelMenuPhotoUpload() {
        RestaurantService restaurantService = mock(RestaurantService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        SummaryService summaryService = mock(SummaryService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramAdminCommandServiceImpl service = new TelegramAdminCommandServiceImpl(
                restaurantService,
                menuItemService,
                orderSessionService,
                restaurantVoteSessionService,
                summaryService,
                userOrderService,
                adminAccessService,
                new TelegramKeyboards(),
                menuPhotoUploadStateService
        );

        LunchUser actor = new LunchUser();
        actor.setId(7L);
        actor.setTelegramUserId(1001L);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(72L);
        when(menuItemService.getRequired(72L)).thenReturn(menuItem);

        TelegramAdminCommandService.AdminCommandResponse start = service.handle(actor, "/menu_photo 72");
        TelegramAdminCommandService.AdminCommandResponse cancel = service.handle(actor, "/menu_photo_cancel");

        assertThat(start.text()).isEqualTo("Send photo for menu item #72");
        assertThat(cancel.text()).isEqualTo("Menu photo upload cancelled");
        verify(menuPhotoUploadStateService).remember(1001L, 72L);
        verify(menuPhotoUploadStateService).clear(1001L);
    }

    @Test
    void shouldUpdateMenuCategoryCommand() {
        RestaurantService restaurantService = mock(RestaurantService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        SummaryService summaryService = mock(SummaryService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramAdminCommandServiceImpl service = new TelegramAdminCommandServiceImpl(
                restaurantService,
                menuItemService,
                orderSessionService,
                restaurantVoteSessionService,
                summaryService,
                userOrderService,
                adminAccessService,
                new TelegramKeyboards(),
                menuPhotoUploadStateService
        );

        LunchUser actor = new LunchUser();
        actor.setId(7L);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        MenuItem menuItem = new MenuItem();
        menuItem.setId(15L);
        menuItem.setName("Bon file");
        menuItem.setRestaurant(restaurant);
        menuItem.setCategory("Grill");
        when(menuItemService.updateCategory(eq(15L), eq(new UpdateMenuItemCategoryRequest("Grill")), eq(7L))).thenReturn(menuItem);

        TelegramAdminCommandService.AdminCommandResponse result = service.handle(actor, "/menu_category 15 | Grill");

        assertThat(result.text()).contains("Menu item category updated").contains("category=Grill");
    }

    @Test
    void shouldBuildOrderControl() {
        RestaurantService restaurantService = mock(RestaurantService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        SummaryService summaryService = mock(SummaryService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramAdminCommandServiceImpl service = new TelegramAdminCommandServiceImpl(
                restaurantService,
                menuItemService,
                orderSessionService,
                restaurantVoteSessionService,
                summaryService,
                userOrderService,
                adminAccessService,
                new TelegramKeyboards(),
                menuPhotoUploadStateService
        );

        LunchUser actor = new LunchUser();
        actor.setId(7L);
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Osh Posh");
        uz.company.lunchbot.entity.OrderSession session = new uz.company.lunchbot.entity.OrderSession();
        session.setId(1L);
        session.setRestaurant(restaurant);
        session.setStatus(uz.company.lunchbot.enums.OrderSessionStatus.OPEN);
        session.setDeadlineAt(java.time.LocalDateTime.of(2026, 5, 5, 11, 30));
        when(orderSessionService.getTodaySession()).thenReturn(java.util.Optional.of(session));
        when(summaryService.buildSummary(1L)).thenReturn(new uz.company.lunchbot.dto.response.SessionSummaryResponse(1L, java.time.LocalDate.of(2026, 5, 5), uz.company.lunchbot.enums.OrderSessionStatus.OPEN, 8L, 2L, 5L, List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", ""));

        TelegramAdminCommandService.AdminCommandResponse response = service.handle(actor, "/order_control");

        assertThat(response.text()).contains("Bugungi order control").contains("Restoran: Osh Posh");
        assertThat(firstCallback(response.keyboard())).isEqualTo("ORDER_EXTEND:1:10");
    }

    private static List<MenuItem> items(Restaurant restaurant, int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> {
                    MenuItem item = new MenuItem();
                    item.setId((long) index);
                    item.setRestaurant(restaurant);
                    item.setName("Meal " + index);
                    item.setPrice(new BigDecimal("10000").add(BigDecimal.valueOf(index)));
                    item.setActive(index % 2 == 0);
                    item.setCategory(index % 2 == 0 ? "Main" : "Dessert");
                    item.setContainerRequired(null);
                    if (index % 3 == 0) {
                        item.setImageUrl("https://example.com/meal-" + index + ".jpg");
                    }
                    return item;
                })
                .toList();
    }

    private static long menuLineCount(String text) {
        return text.lines().filter(line -> line.startsWith("#")).count();
    }

    private static String firstCallback(Object keyboard) {
        if (!(keyboard instanceof Map<?, ?> map)) {
            return null;
        }
        Object rowsValue = map.get("inline_keyboard");
        if (!(rowsValue instanceof List<?> rows) || rows.isEmpty()) {
            return null;
        }
        Object rowValue = rows.get(0);
        if (!(rowValue instanceof List<?> buttons) || buttons.isEmpty()) {
            return null;
        }
        Object buttonValue = buttons.get(0);
        if (!(buttonValue instanceof Map<?, ?> button)) {
            return null;
        }
        Object callbackData = button.get("callback_data");
        return callbackData == null ? null : callbackData.toString();
    }
}
