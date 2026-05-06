package uz.company.lunchbot.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.bot.TelegramApiClient;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.config.TelegramBotProperties;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.RoundingStrategy;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.service.calculation.ContainerPricingService;
import uz.company.lunchbot.service.calculation.impl.ContainerPricingServiceImpl;
import uz.company.lunchbot.service.impl.NotificationServiceImpl;

class NotificationServiceImplTest {

    @Test
    void shouldSendGroupTextToConfiguredLunchGroup() {
        TelegramApiClient telegramApiClient = mock(TelegramApiClient.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        UserService userService = mock(UserService.class);

        NotificationServiceImpl service = new NotificationServiceImpl(
                telegramApiClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramBotProperties(true, "token", "bot"),
                properties(),
                menuItemService,
                userService,
                new ContainerPricingServiceImpl()
        );

        service.sendGroupText("Vote message", Map.of());

        verify(telegramApiClient).sendMessageAndGetMessageId(1L, "Vote message", Map.of());
    }

    @Test
    void shouldSendTodaysMenuAsReadOnly() {
        TelegramApiClient telegramApiClient = mock(TelegramApiClient.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        UserService userService = mock(UserService.class);

        NotificationServiceImpl service = new NotificationServiceImpl(
                telegramApiClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramBotProperties(true, "token", "bot"),
                properties(),
                menuItemService,
                userService,
                new ContainerPricingServiceImpl()
        );

        OrderSession session = session(1L);
        MenuItem first = menuItem(1L, "To'y oshi", "33000");
        first.setCategory("Osh");
        MenuItem second = menuItem(2L, "Achichuk", "12000");
        second.setCategory("Salad");
        when(menuItemService.getActiveMenu(1L)).thenReturn(List.of(first, second));

        service.sendTodayMenu(50L, session);

        verify(telegramApiClient).sendMessage(eq(50L), argThat(text -> text.contains("Osh:") && text.contains("Salad:")), isNull());
        verify(telegramApiClient, never()).sendPhoto(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSendPlaceOrderButtonsForRequestedRestaurantMenu() {
        TelegramApiClient telegramApiClient = mock(TelegramApiClient.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        UserService userService = mock(UserService.class);

        NotificationServiceImpl service = new NotificationServiceImpl(
                telegramApiClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramBotProperties(true, "token", "bot"),
                properties(),
                menuItemService,
                userService,
                new ContainerPricingServiceImpl()
        );

        OrderSession session = session(7L);
        session.setId(99L);
        MenuItem burger = menuItem(11L, "Burger", "45000");
        burger.setCategory("Burgers");
        MenuItem twister = menuItem(12L, "Twister", "39000");
        twister.setCategory("Wraps");
        when(menuItemService.getActiveMenu(7L)).thenReturn(List.of(burger, twister));
        LunchUser user = new LunchUser();
        user.setPrivateChatId(60L);
        user.setLanguage(UserLanguage.UZ);
        when(userService.findByPrivateChatId(60L)).thenReturn(java.util.Optional.of(user));

        service.sendMenuSelection(60L, session);

        verify(menuItemService).getActiveMenu(7L);
        verify(telegramApiClient).sendMessage(eq(60L), anyString(), argThat(markup -> {
            if (!(markup instanceof Map<?, ?> map)) {
                return false;
            }
            Object inlineKeyboard = map.get("inline_keyboard");
            if (!(inlineKeyboard instanceof List<?> rows) || rows.size() < 2) {
                return false;
            }
            String firstCallback = callbackData(rows.get(0));
            String secondCallback = callbackData(rows.get(1));
            return "MENU_CATEGORY:99:0:0".equals(firstCallback) && "MENU_CATEGORY:99:1:0".equals(secondCallback);
        }));
    }

    @Test
    void shouldSendOnlyCategoryItemsWithPagination() {
        TelegramApiClient telegramApiClient = mock(TelegramApiClient.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        UserService userService = mock(UserService.class);

        NotificationServiceImpl service = new NotificationServiceImpl(
                telegramApiClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramBotProperties(true, "token", "bot"),
                properties(),
                menuItemService,
                userService,
                new ContainerPricingServiceImpl()
        );

        OrderSession session = session(7L);
        session.setId(55L);
        List<MenuItem> items = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> {
                    MenuItem item = menuItem((long) index, "Burger " + index, "45000");
                    item.setCategory("Burgers");
                    return item;
                })
                .toList();
        when(menuItemService.getActiveMenu(7L)).thenReturn(items);

        LunchUser user = new LunchUser();
        user.setPrivateChatId(60L);
        user.setLanguage(UserLanguage.UZ);
        when(userService.findByPrivateChatId(60L)).thenReturn(java.util.Optional.of(user));

        service.sendMenuCategorySelection(60L, session, 0, 1);

        verify(telegramApiClient).sendMessage(eq(60L), argThat(text -> text.contains("page 2/2") && text.contains("Burger 11") && text.contains("Burger 12")), argThat(markup -> {
            if (!(markup instanceof Map<?, ?> map)) {
                return false;
            }
            Object inlineKeyboard = map.get("inline_keyboard");
            if (!(inlineKeyboard instanceof List<?> rows) || rows.size() < 3) {
                return false;
            }
            return "MENU_ITEM_PREVIEW:55:11".equals(callbackData(rows.get(0)))
                    && "MENU_ITEM_PREVIEW:55:12".equals(callbackData(rows.get(1)))
                    && "MENU_SELECTION:55".equals(callbackData(rows.get(rows.size() - 2)));
        }));
    }

    @Test
    void shouldFallbackToTextPreviewWhenImageIsMissing() {
        TelegramApiClient telegramApiClient = mock(TelegramApiClient.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        UserService userService = mock(UserService.class);
        ContainerPricingService containerPricingService = new ContainerPricingServiceImpl();

        NotificationServiceImpl service = new NotificationServiceImpl(
                telegramApiClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramBotProperties(true, "token", "bot"),
                properties(),
                menuItemService,
                userService,
                containerPricingService
        );

        OrderSession session = session(1L);
        session.setId(99L);
        Restaurant restaurant = session.getRestaurant();
        restaurant.setContainerEnabled(true);
        restaurant.setDefaultContainerPrice(new BigDecimal("2000"));

        MenuItem item = menuItem(11L, "Burger", "45000");
        item.setRestaurant(restaurant);

        LunchUser user = new LunchUser();
        user.setPrivateChatId(60L);
        user.setLanguage(UserLanguage.UZ);
        when(userService.findByPrivateChatId(60L)).thenReturn(java.util.Optional.of(user));

        service.sendMenuItemPreview(60L, session, item);

        verify(telegramApiClient, never()).sendPhoto(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.any());
        verify(telegramApiClient).sendMessage(eq(60L), anyString(), argThat(markup -> {
            if (!(markup instanceof Map<?, ?> map)) {
                return false;
            }
            Object inlineKeyboard = map.get("inline_keyboard");
            if (!(inlineKeyboard instanceof List<?> rows) || rows.isEmpty()) {
                return false;
            }
            return "ORDER_ITEM:99:11".equals(callbackData(rows.get(0)));
        }));
    }

    @Test
    void shouldFallbackToTextWhenPhotoSendFails() {
        TelegramApiClient telegramApiClient = mock(TelegramApiClient.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        UserService userService = mock(UserService.class);

        NotificationServiceImpl service = new NotificationServiceImpl(
                telegramApiClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramBotProperties(true, "token", "bot"),
                properties(),
                menuItemService,
                userService,
                new ContainerPricingServiceImpl()
        );

        OrderSession session = session(1L);
        session.setId(77L);
        Restaurant restaurant = session.getRestaurant();
        restaurant.setContainerEnabled(false);

        MenuItem item = menuItem(15L, "Twister", "39000");
        item.setRestaurant(restaurant);
        item.setImageUrl("https://example.com/twister.jpg");

        LunchUser user = new LunchUser();
        user.setPrivateChatId(60L);
        user.setLanguage(UserLanguage.UZ);
        when(userService.findByPrivateChatId(60L)).thenReturn(java.util.Optional.of(user));
        when(telegramApiClient.sendPhoto(eq(60L), eq("https://example.com/twister.jpg"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(false);

        service.sendMenuItemPreview(60L, session, item);

        verify(telegramApiClient).sendPhoto(eq(60L), eq("https://example.com/twister.jpg"), anyString(), org.mockito.ArgumentMatchers.any());
        verify(telegramApiClient).sendMessage(eq(60L), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPreferTelegramFileIdBeforeImageUrl() {
        TelegramApiClient telegramApiClient = mock(TelegramApiClient.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        UserService userService = mock(UserService.class);

        NotificationServiceImpl service = new NotificationServiceImpl(
                telegramApiClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramBotProperties(true, "token", "bot"),
                properties(),
                menuItemService,
                userService,
                new ContainerPricingServiceImpl()
        );

        OrderSession session = session(1L);
        session.setId(88L);
        Restaurant restaurant = session.getRestaurant();
        restaurant.setContainerEnabled(false);

        MenuItem item = menuItem(16L, "Lavash", "35000");
        item.setRestaurant(restaurant);
        item.setTelegramImageFileId("telegram-file-id");
        item.setImageUrl("https://example.com/lavash.jpg");

        LunchUser user = new LunchUser();
        user.setPrivateChatId(60L);
        user.setLanguage(UserLanguage.UZ);
        when(userService.findByPrivateChatId(60L)).thenReturn(java.util.Optional.of(user));
        when(telegramApiClient.sendPhoto(eq(60L), eq("telegram-file-id"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);

        service.sendMenuItemPreview(60L, session, item);

        verify(telegramApiClient).sendPhoto(eq(60L), eq("telegram-file-id"), anyString(), org.mockito.ArgumentMatchers.any());
        verify(telegramApiClient, never()).sendPhoto(eq(60L), eq("https://example.com/lavash.jpg"), anyString(), org.mockito.ArgumentMatchers.any());
        verify(telegramApiClient, never()).sendMessage(eq(60L), anyString(), org.mockito.ArgumentMatchers.any());
    }

    private static String callbackData(Object row) {
        if (!(row instanceof List<?> buttons) || buttons.isEmpty()) {
            return null;
        }
        Object button = buttons.get(0);
        if (!(button instanceof Map<?, ?> map)) {
            return null;
        }
        Object callback = map.get("callback_data");
        return callback == null ? null : callback.toString();
    }

    private static MenuItem menuItem(Long id, String name, String price) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return item;
    }

    private static OrderSession session(Long restaurantId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Osh Posh");

        OrderSession session = new OrderSession();
        session.setRestaurant(restaurant);
        session.setOrderDate(LocalDate.of(2026, 5, 5));
        session.setStatus(OrderSessionStatus.OPEN);
        session.setDeadlineAt(LocalDateTime.of(2026, 5, 5, 11, 30));
        return session;
    }

    private static LunchProperties properties() {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                LocalTime.of(11, 30),
                RoundingStrategy.CEIL_TO_100,
                new LunchProperties.Scheduler(true, "", "", ""),
                new LunchProperties.RestaurantVoting(false, "", "", 5),
                new LunchProperties.Payment(true, "8600 1111 2222 3333", "Toxirjon Sadullayev", true),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L)
        );
    }
}
