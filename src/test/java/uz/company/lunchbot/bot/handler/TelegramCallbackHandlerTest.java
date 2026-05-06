package uz.company.lunchbot.bot.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.service.MenuItemService;
import uz.company.lunchbot.service.NotificationService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.PaymentService;
import uz.company.lunchbot.service.RestaurantVoteSessionService;
import uz.company.lunchbot.service.SummaryService;
import uz.company.lunchbot.service.TelegramAdminCommandService;
import uz.company.lunchbot.service.TelegramRegistrationStateService;
import uz.company.lunchbot.service.UserOrderService;
import uz.company.lunchbot.service.UserService;
import uz.company.lunchbot.util.CallbackDataParser;

class TelegramCallbackHandlerTest {

    @Test
    void shouldApprovePendingUserAndShowMenu() {
        CallbackDataParser callbackDataParser = mock(CallbackDataParser.class);
        UserService userService = mock(UserService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);

        TelegramCallbackHandler handler = new TelegramCallbackHandler(
                callbackDataParser,
                userService,
                userOrderService,
                orderSessionService,
                menuItemService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                restaurantVoteSessionService,
                paymentService,
                telegramAdminCommandService,
                registrationStateService,
                new TelegramKeyboards()
        );

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(callbackQuery.getData()).thenReturn("APPROVE_USER:9");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(500L);
        when(callbackQuery.getFrom()).thenReturn(telegramUser);
        when(telegramUser.getId()).thenReturn(1001L);
        when(callbackQuery.getId()).thenReturn("cb-1");
        when(callbackDataParser.parse("APPROVE_USER:9")).thenReturn(new CallbackDataParser.ParsedCallback("APPROVE_USER", java.util.List.of("9")));

        LunchUser actor = new LunchUser();
        actor.setId(1L);
        actor.setStatus(UserStatus.APPROVED);
        actor.setLanguage(UserLanguage.UZ);
        when(userService.getApprovedUserByTelegramUserId(1001L)).thenReturn(actor);

        LunchUser approved = new LunchUser();
        approved.setId(9L);
        approved.setPrivateChatId(700L);
        approved.setLanguage(UserLanguage.RU);
        approved.setStatus(UserStatus.APPROVED);
        approved.setFirstName("Ivan");
        when(userService.approve(9L, 1L)).thenReturn(approved);

        handler.handle(callbackQuery);

        verify(notificationService).sendApprovedMainMenu(eq(approved), any(String.class));
    }

    @Test
    void shouldSendPreviewForMenuItemWithoutImage() {
        CallbackDataParser callbackDataParser = mock(CallbackDataParser.class);
        UserService userService = mock(UserService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);

        TelegramCallbackHandler handler = new TelegramCallbackHandler(
                callbackDataParser,
                userService,
                userOrderService,
                orderSessionService,
                menuItemService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                restaurantVoteSessionService,
                paymentService,
                telegramAdminCommandService,
                registrationStateService,
                new TelegramKeyboards()
        );

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(callbackQuery.getData()).thenReturn("MENU_ITEM_PREVIEW:44:77");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(500L);
        when(callbackQuery.getFrom()).thenReturn(telegramUser);
        when(telegramUser.getId()).thenReturn(1001L);
        when(callbackQuery.getId()).thenReturn("cb-preview");
        when(callbackDataParser.parse("MENU_ITEM_PREVIEW:44:77")).thenReturn(new CallbackDataParser.ParsedCallback("MENU_ITEM_PREVIEW", java.util.List.of("44", "77")));

        LunchUser actor = new LunchUser();
        actor.setId(1L);
        actor.setStatus(UserStatus.APPROVED);
        actor.setLanguage(UserLanguage.UZ);
        when(userService.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.of(actor));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(9L);

        OrderSession session = new OrderSession();
        session.setId(44L);
        session.setRestaurant(restaurant);
        session.setStatus(OrderSessionStatus.OPEN);
        when(orderSessionService.getActiveOrderingSession()).thenReturn(session);

        MenuItem item = new MenuItem();
        item.setId(77L);
        item.setRestaurant(restaurant);
        item.setActive(true);
        when(menuItemService.getRequired(77L)).thenReturn(item);

        handler.handle(callbackQuery);

        verify(notificationService).sendMenuItemPreview(500L, session, item);
    }

    @Test
    void shouldEditMenuPageOnPaginationCallback() {
        CallbackDataParser callbackDataParser = mock(CallbackDataParser.class);
        UserService userService = mock(UserService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);

        TelegramCallbackHandler handler = new TelegramCallbackHandler(
                callbackDataParser,
                userService,
                userOrderService,
                orderSessionService,
                menuItemService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                restaurantVoteSessionService,
                paymentService,
                telegramAdminCommandService,
                registrationStateService,
                new TelegramKeyboards()
        );

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(callbackQuery.getData()).thenReturn("MENU_PAGE:2:1");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(500L);
        when(message.getMessageId()).thenReturn(321);
        when(callbackQuery.getFrom()).thenReturn(telegramUser);
        when(telegramUser.getId()).thenReturn(1001L);
        when(callbackQuery.getId()).thenReturn("cb-page");
        when(callbackDataParser.parse("MENU_PAGE:2:1")).thenReturn(new CallbackDataParser.ParsedCallback("MENU_PAGE", java.util.List.of("2", "1")));

        LunchUser actor = new LunchUser();
        actor.setId(1L);
        actor.setStatus(UserStatus.APPROVED);
        when(userService.getApprovedUserByTelegramUserId(1001L)).thenReturn(actor);
        when(telegramAdminCommandService.buildMenuPage(actor, 2L, 1))
                .thenReturn(new TelegramAdminCommandService.AdminCommandResponse("Tarnov menu — page 2/3", java.util.Map.of()));

        handler.handle(callbackQuery);

        verify(notificationService).editText(500L, 321L, "Tarnov menu — page 2/3", java.util.Map.of());
    }

    @Test
    void shouldSendCategoryItemsOnMenuCategoryCallback() {
        CallbackDataParser callbackDataParser = mock(CallbackDataParser.class);
        UserService userService = mock(UserService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);

        TelegramCallbackHandler handler = new TelegramCallbackHandler(
                callbackDataParser,
                userService,
                userOrderService,
                orderSessionService,
                menuItemService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                restaurantVoteSessionService,
                paymentService,
                telegramAdminCommandService,
                registrationStateService,
                new TelegramKeyboards()
        );

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(callbackQuery.getData()).thenReturn("MENU_CATEGORY:44:1:0");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(500L);
        when(callbackQuery.getFrom()).thenReturn(telegramUser);
        when(telegramUser.getId()).thenReturn(1001L);
        when(callbackQuery.getId()).thenReturn("cb-category");
        when(callbackDataParser.parse("MENU_CATEGORY:44:1:0")).thenReturn(new CallbackDataParser.ParsedCallback("MENU_CATEGORY", java.util.List.of("44", "1", "0")));

        LunchUser actor = new LunchUser();
        actor.setId(1L);
        actor.setStatus(UserStatus.APPROVED);
        actor.setLanguage(UserLanguage.UZ);
        when(userService.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.of(actor));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(9L);

        OrderSession session = new OrderSession();
        session.setId(44L);
        session.setRestaurant(restaurant);
        session.setStatus(OrderSessionStatus.OPEN);
        when(orderSessionService.getActiveOrderingSession()).thenReturn(session);

        handler.handle(callbackQuery);

        verify(notificationService).sendMenuCategorySelection(500L, session, 1, 0);
    }

    @Test
    void shouldBlockRestaurantVoteForUnregisteredUser() {
        CallbackDataParser callbackDataParser = mock(CallbackDataParser.class);
        UserService userService = mock(UserService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);

        TelegramCallbackHandler handler = new TelegramCallbackHandler(
                callbackDataParser,
                userService,
                userOrderService,
                orderSessionService,
                menuItemService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                restaurantVoteSessionService,
                paymentService,
                telegramAdminCommandService,
                registrationStateService,
                new TelegramKeyboards()
        );

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(callbackQuery.getData()).thenReturn("RESTAURANT_VOTE:10:1");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(-100L);
        when(callbackQuery.getFrom()).thenReturn(telegramUser);
        when(telegramUser.getId()).thenReturn(1001L);
        when(callbackQuery.getId()).thenReturn("cb-vote-1");
        when(callbackDataParser.parse("RESTAURANT_VOTE:10:1")).thenReturn(new CallbackDataParser.ParsedCallback("RESTAURANT_VOTE", java.util.List.of("10", "1")));
        when(userService.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.empty());

        handler.handle(callbackQuery);

        verify(notificationService).answerCallback("cb-vote-1", "Avval botga /start bosing va ro'yxatdan o'ting.");
    }

    @Test
    void shouldBlockRestaurantVoteForPendingUser() {
        CallbackDataParser callbackDataParser = mock(CallbackDataParser.class);
        UserService userService = mock(UserService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);

        TelegramCallbackHandler handler = new TelegramCallbackHandler(
                callbackDataParser,
                userService,
                userOrderService,
                orderSessionService,
                menuItemService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                restaurantVoteSessionService,
                paymentService,
                telegramAdminCommandService,
                registrationStateService,
                new TelegramKeyboards()
        );

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(callbackQuery.getData()).thenReturn("RESTAURANT_VOTE:10:1");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(-100L);
        when(callbackQuery.getFrom()).thenReturn(telegramUser);
        when(telegramUser.getId()).thenReturn(1001L);
        when(callbackQuery.getId()).thenReturn("cb-vote-2");
        when(callbackDataParser.parse("RESTAURANT_VOTE:10:1")).thenReturn(new CallbackDataParser.ParsedCallback("RESTAURANT_VOTE", java.util.List.of("10", "1")));

        LunchUser user = new LunchUser();
        user.setId(7L);
        user.setStatus(UserStatus.PENDING);
        when(userService.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.of(user));

        handler.handle(callbackQuery);

        verify(notificationService).answerCallback("cb-vote-2", "Ro'yxatdan o'tish so'rovingiz admin tasdig'ini kutmoqda.");
    }

    @Test
    void shouldBlockRestaurantVoteForBlockedUser() {
        CallbackDataParser callbackDataParser = mock(CallbackDataParser.class);
        UserService userService = mock(UserService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);

        TelegramCallbackHandler handler = new TelegramCallbackHandler(
                callbackDataParser,
                userService,
                userOrderService,
                orderSessionService,
                menuItemService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                restaurantVoteSessionService,
                paymentService,
                telegramAdminCommandService,
                registrationStateService,
                new TelegramKeyboards()
        );

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(callbackQuery.getData()).thenReturn("RESTAURANT_VOTE:10:1");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(-100L);
        when(callbackQuery.getFrom()).thenReturn(telegramUser);
        when(telegramUser.getId()).thenReturn(1001L);
        when(callbackQuery.getId()).thenReturn("cb-vote-3");
        when(callbackDataParser.parse("RESTAURANT_VOTE:10:1")).thenReturn(new CallbackDataParser.ParsedCallback("RESTAURANT_VOTE", java.util.List.of("10", "1")));

        LunchUser user = new LunchUser();
        user.setId(7L);
        user.setStatus(UserStatus.BLOCKED);
        when(userService.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.of(user));

        handler.handle(callbackQuery);

        verify(notificationService).answerCallback("cb-vote-3", "Sizga botdan foydalanish ruxsati berilmagan.");
    }

    @Test
    void shouldSaveRestaurantVoteForApprovedUser() {
        CallbackDataParser callbackDataParser = mock(CallbackDataParser.class);
        UserService userService = mock(UserService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);

        TelegramCallbackHandler handler = new TelegramCallbackHandler(
                callbackDataParser,
                userService,
                userOrderService,
                orderSessionService,
                menuItemService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                restaurantVoteSessionService,
                paymentService,
                telegramAdminCommandService,
                registrationStateService,
                new TelegramKeyboards()
        );

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(callbackQuery.getData()).thenReturn("RESTAURANT_VOTE:10:1");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(-100L);
        when(callbackQuery.getFrom()).thenReturn(telegramUser);
        when(telegramUser.getId()).thenReturn(1001L);
        when(callbackQuery.getId()).thenReturn("cb-vote-4");
        when(callbackDataParser.parse("RESTAURANT_VOTE:10:1")).thenReturn(new CallbackDataParser.ParsedCallback("RESTAURANT_VOTE", java.util.List.of("10", "1")));

        LunchUser user = new LunchUser();
        user.setId(7L);
        user.setStatus(UserStatus.APPROVED);
        user.setLanguage(UserLanguage.UZ);
        when(userService.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.of(user));

        handler.handle(callbackQuery);

        verify(restaurantVoteSessionService).castVote(1001L, 10L, 1L);
        verify(notificationService).answerCallback("cb-vote-4", "Ovozingiz saqlandi.");
    }
}
