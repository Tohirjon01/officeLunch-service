package uz.company.lunchbot.bot.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.dto.request.TelegramRegistrationRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemImageRequest;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.RestaurantVoteSession;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.service.MenuItemService;
import uz.company.lunchbot.service.MenuPhotoUploadStateService;
import uz.company.lunchbot.service.NotificationService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.PaymentService;
import uz.company.lunchbot.service.RestaurantVoteSessionService;
import uz.company.lunchbot.service.SummaryService;
import uz.company.lunchbot.service.TelegramAdminCommandService;
import uz.company.lunchbot.service.TelegramRegistrationStateService;
import uz.company.lunchbot.service.UserOrderService;
import uz.company.lunchbot.service.UserService;

class TelegramMessageHandlerTest {

    @Test
    void shouldAskLanguageOnStartForNewUser() {
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramMessageHandler handler = new TelegramMessageHandler(
                userService,
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                telegramAdminCommandService,
                restaurantVoteSessionService,
                paymentService,
                registrationStateService,
                new TelegramKeyboards(),
                menuItemService,
                menuPhotoUploadStateService
        );

        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.Chat chat = mock(org.telegram.telegrambots.meta.api.objects.Chat.class);
        org.telegram.telegrambots.meta.api.objects.User from = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(message.getChat()).thenReturn(chat);
        when(chat.getType()).thenReturn("private");
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        when(message.getChatId()).thenReturn(100L);
        when(message.getFrom()).thenReturn(from);
        when(from.getId()).thenReturn(1001L);
        when(userService.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.empty());

        handler.handle(message);

        verify(notificationService).sendPrivateText(eq(100L), eq(new TelegramMessages().chooseLanguage()), any());
    }

    @Test
    void shouldRegisterPendingUserAfterContactShare() {
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramMessageHandler handler = new TelegramMessageHandler(
                userService,
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                telegramAdminCommandService,
                restaurantVoteSessionService,
                paymentService,
                registrationStateService,
                new TelegramKeyboards(),
                menuItemService,
                menuPhotoUploadStateService
        );

        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.Chat chat = mock(org.telegram.telegrambots.meta.api.objects.Chat.class);
        org.telegram.telegrambots.meta.api.objects.User from = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        Contact contact = mock(Contact.class);
        when(message.getChat()).thenReturn(chat);
        when(chat.getType()).thenReturn("private");
        when(message.hasContact()).thenReturn(true);
        when(message.getContact()).thenReturn(contact);
        when(contact.getUserId()).thenReturn(1001L);
        when(contact.getPhoneNumber()).thenReturn("+998901112233");
        when(message.getChatId()).thenReturn(100L);
        when(message.getFrom()).thenReturn(from);
        when(from.getId()).thenReturn(1001L);
        when(from.getUserName()).thenReturn("ali_user");
        when(from.getFirstName()).thenReturn("Ali");
        when(from.getLastName()).thenReturn("Valiyev");
        when(userService.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.empty());
        when(registrationStateService.getLanguage(1001L)).thenReturn(java.util.Optional.of(UserLanguage.UZ));

        LunchUser pendingUser = new LunchUser();
        pendingUser.setId(1L);
        pendingUser.setStatus(UserStatus.PENDING);
        pendingUser.setLanguage(UserLanguage.UZ);
        when(userService.completeTelegramRegistration(any(TelegramRegistrationRequest.class), eq(UserLanguage.UZ))).thenReturn(pendingUser);

        handler.handle(message);

        verify(userService).completeTelegramRegistration(any(TelegramRegistrationRequest.class), eq(UserLanguage.UZ));
        verify(notificationService).notifyAdminsAboutPendingUser(pendingUser);
    }

    @Test
    void shouldUseCurrentSessionRestaurantForPlaceOrder() {
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramMessageHandler handler = new TelegramMessageHandler(
                userService,
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                telegramAdminCommandService,
                restaurantVoteSessionService,
                paymentService,
                registrationStateService,
                new TelegramKeyboards(),
                menuItemService,
                menuPhotoUploadStateService
        );

        Restaurant restaurant = new Restaurant();
        restaurant.setId(42L);

        LunchUser user = new LunchUser();
        user.setLanguage(UserLanguage.UZ);

        OrderSession session = new OrderSession();
        session.setRestaurant(restaurant);
        session.setStatus(OrderSessionStatus.OPEN);

        when(restaurantVoteSessionService.getTodaySession()).thenReturn(java.util.Optional.empty());
        when(orderSessionService.getTodaySession()).thenReturn(java.util.Optional.of(session));
        when(orderSessionService.getActiveOrderingSession()).thenReturn(session);

        ReflectionTestUtils.invokeMethod(handler, "handlePlaceOrder", 100L, user);

        verify(notificationService).sendMenuSelection(100L, session);
    }

    @Test
    void shouldSavePendingMenuPhotoInsteadOfSubmittingReceipt() {
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramMessageHandler handler = new TelegramMessageHandler(
                userService,
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                telegramAdminCommandService,
                restaurantVoteSessionService,
                paymentService,
                registrationStateService,
                new TelegramKeyboards(),
                menuItemService,
                menuPhotoUploadStateService
        );

        Message message = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.Chat chat = mock(org.telegram.telegrambots.meta.api.objects.Chat.class);
        org.telegram.telegrambots.meta.api.objects.User from = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        org.telegram.telegrambots.meta.api.objects.PhotoSize small = mock(org.telegram.telegrambots.meta.api.objects.PhotoSize.class);
        org.telegram.telegrambots.meta.api.objects.PhotoSize large = mock(org.telegram.telegrambots.meta.api.objects.PhotoSize.class);
        when(message.getChat()).thenReturn(chat);
        when(chat.getType()).thenReturn("private");
        when(message.hasPhoto()).thenReturn(true);
        when(message.hasDocument()).thenReturn(false);
        when(message.getPhoto()).thenReturn(java.util.List.of(small, large));
        when(large.getFileId()).thenReturn("largest-file-id");
        when(message.getChatId()).thenReturn(100L);
        when(message.getFrom()).thenReturn(from);
        when(from.getId()).thenReturn(1001L);
        when(menuPhotoUploadStateService.getPendingMenuItemId(1001L)).thenReturn(java.util.Optional.of(72L));

        LunchUser actor = new LunchUser();
        actor.setId(7L);
        when(userService.getApprovedUserByTelegramUserId(1001L)).thenReturn(actor);

        MenuItem item = new MenuItem();
        item.setId(72L);
        item.setImageUrl("https://example.com/old.jpg");
        when(menuItemService.getRequired(72L)).thenReturn(item);

        handler.handle(message);

        verify(menuItemService).updateImage(eq(72L), eq(new UpdateMenuItemImageRequest("https://example.com/old.jpg", "largest-file-id")), eq(7L));
        verify(menuPhotoUploadStateService).clear(1001L);
        verify(notificationService).sendPrivateText(100L, "Photo saved for menu item #72", null);
        verify(paymentService, org.mockito.Mockito.never()).submitReceipt(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldBlockTodaysMenuDuringVoting() {
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramMessageHandler handler = new TelegramMessageHandler(
                userService,
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                telegramAdminCommandService,
                restaurantVoteSessionService,
                paymentService,
                registrationStateService,
                new TelegramKeyboards(),
                menuItemService,
                menuPhotoUploadStateService
        );

        LunchUser user = new LunchUser();
        user.setId(1L);
        user.setTelegramUserId(1001L);
        user.setLanguage(UserLanguage.UZ);
        RestaurantVoteSession voteSession = new RestaurantVoteSession();
        voteSession.setStatus(RestaurantVoteSessionStatus.OPEN);
        when(restaurantVoteSessionService.getTodaySession()).thenReturn(java.util.Optional.of(voteSession));

        ReflectionTestUtils.invokeMethod(handler, "handleTodaysMenu", 100L, user);

        verify(notificationService).sendPrivateText(100L, "⏳ Hozir restoran tanlash jarayoni davom etmoqda. Buyurtma voting tugagandan keyin ochiladi.", null);
    }

    @Test
    void shouldBlockPlaceOrderWhenNoSessionExists() {
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramMessageHandler handler = new TelegramMessageHandler(
                userService,
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                telegramAdminCommandService,
                restaurantVoteSessionService,
                paymentService,
                registrationStateService,
                new TelegramKeyboards(),
                menuItemService,
                menuPhotoUploadStateService
        );

        LunchUser user = new LunchUser();
        user.setId(1L);
        user.setTelegramUserId(1001L);
        user.setLanguage(UserLanguage.UZ);
        when(restaurantVoteSessionService.getTodaySession()).thenReturn(java.util.Optional.empty());
        when(orderSessionService.getTodaySession()).thenReturn(java.util.Optional.empty());

        ReflectionTestUtils.invokeMethod(handler, "handlePlaceOrder", 100L, user);

        verify(notificationService).sendPrivateText(100L, "⏳ Hozircha buyurtma ochilmagan. Buyurtma restoran tanlangandan keyin ochiladi.", null);
    }

    @Test
    void shouldBlockMyOrderAfterOrderClosed() {
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        UserOrderService userOrderService = mock(UserOrderService.class);
        SummaryService summaryService = mock(SummaryService.class);
        NotificationService notificationService = mock(NotificationService.class);
        TelegramAdminCommandService telegramAdminCommandService = mock(TelegramAdminCommandService.class);
        RestaurantVoteSessionService restaurantVoteSessionService = mock(RestaurantVoteSessionService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TelegramRegistrationStateService registrationStateService = mock(TelegramRegistrationStateService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        MenuPhotoUploadStateService menuPhotoUploadStateService = mock(MenuPhotoUploadStateService.class);

        TelegramMessageHandler handler = new TelegramMessageHandler(
                userService,
                orderSessionService,
                userOrderService,
                summaryService,
                notificationService,
                new TelegramMessages(),
                telegramAdminCommandService,
                restaurantVoteSessionService,
                paymentService,
                registrationStateService,
                new TelegramKeyboards(),
                menuItemService,
                menuPhotoUploadStateService
        );

        LunchUser user = new LunchUser();
        user.setId(1L);
        user.setTelegramUserId(1001L);
        user.setLanguage(UserLanguage.UZ);
        when(userService.getApprovedUserByTelegramUserId(1001L)).thenReturn(user);
        when(restaurantVoteSessionService.getTodaySession()).thenReturn(java.util.Optional.empty());
        OrderSession session = new OrderSession();
        session.setStatus(OrderSessionStatus.CLOSED);
        when(orderSessionService.getTodaySession()).thenReturn(java.util.Optional.of(session));

        ReflectionTestUtils.invokeMethod(handler, "handleMyOrder", 100L, 1001L, UserLanguage.UZ);

        verify(notificationService).sendPrivateText(100L, "🔒 Bugungi buyurtma yopilgan. Keyingi buyurtma yangi votingdan keyin ochiladi.", null);
    }
}
