package uz.company.lunchbot.service.notification;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.bot.TelegramApiClient;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.config.TelegramBotProperties;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.service.MenuItemService;
import uz.company.lunchbot.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final TelegramApiClient telegramApiClient;
    private final TelegramMessages telegramMessages;
    private final TelegramKeyboards telegramKeyboards;
    private final TelegramBotProperties telegramBotProperties;
    private final LunchProperties lunchProperties;
    private final MenuItemService menuItemService;
    private final UserService userService;

    public void sendPrivateText(Long chatId, String text, Object keyboard) {
        if (chatId == null) {
            return;
        }
        telegramApiClient.sendMessage(chatId, text, keyboard);
    }

    public void answerCallback(String callbackQueryId, String text) {
        telegramApiClient.answerCallbackQuery(callbackQueryId, text);
    }

    public void sendApprovedMainMenu(LunchUser user, String text) {
        sendPrivateText(user.getPrivateChatId(), text, telegramKeyboards.mainMenu(isAdmin(user)));
    }

    public void sendTodayMenu(Long chatId, OrderSession session) {
        List<MenuItem> menuItems = menuItemService.getActiveMenu(session.getRestaurant().getId());
        sendPrivateText(chatId, telegramMessages.todayMenu(session, menuItems), telegramKeyboards.menuSelection(menuItems));
    }

    public void sendMenuSelection(Long chatId, Long restaurantId) {
        List<MenuItem> menuItems = menuItemService.getActiveMenu(restaurantId);
        sendPrivateText(chatId, telegramMessages.chooseMenuItem(), telegramKeyboards.menuSelection(menuItems));
    }

    public void notifyAdminsAboutPendingUser(LunchUser pendingUser) {
        userService.getApprovedAdmins().stream()
                .filter(admin -> admin.getPrivateChatId() != null)
                .forEach(admin -> sendPrivateText(
                        admin.getPrivateChatId(),
                        telegramMessages.pendingUserCard(pendingUser),
                        telegramKeyboards.pendingUserActions(pendingUser.getId())));
    }

    public void sendGroupOpenAnnouncement(OrderSession session) {
        telegramApiClient.sendMessage(
                lunchProperties.groupChatId(),
                telegramMessages.groupOpenAnnouncement(session),
                telegramKeyboards.groupPlaceOrderButton(telegramBotProperties.username()));
    }

    public void sendReminder(OrderSession session, List<LunchUser> notRespondedUsers) {
        telegramApiClient.sendMessage(lunchProperties.groupChatId(), telegramMessages.notRespondedReminder(session, notRespondedUsers), null);
        notRespondedUsers.stream()
                .filter(user -> user.getPrivateChatId() != null)
                .forEach(user -> sendPrivateText(user.getPrivateChatId(), telegramMessages.privateReminder(session), null));
    }

    public void sendClosedSummary(SessionSummaryResponse summary, Long sessionId) {
        telegramApiClient.sendMessage(
                lunchProperties.groupChatId(),
                telegramMessages.sessionClosedShort(summary.orderedCount(), summary.skippedCount(), summary.noResponseCount()),
                telegramKeyboards.adminSessionActions(sessionId));
        telegramApiClient.sendMessage(lunchProperties.groupChatId(), summary.groupSummaryText(), null);
    }

    public void sendConfirmedSummary(SessionSummaryResponse summary) {
        telegramApiClient.sendMessage(lunchProperties.groupChatId(), telegramMessages.sessionConfirmed(), null);
        telegramApiClient.sendMessage(lunchProperties.groupChatId(), summary.restaurantOrderText(), null);
    }

    public void sendPendingUsersList(Long chatId, List<LunchUser> pendingUsers) {
        if (pendingUsers.isEmpty()) {
            sendPrivateText(chatId, telegramMessages.noPendingUsers(), null);
            return;
        }

        pendingUsers.forEach(user -> sendPrivateText(chatId, telegramMessages.pendingUserCard(user), telegramKeyboards.pendingUserActions(user.getId())));
    }

    private boolean isAdmin(LunchUser user) {
        return user.getRole() == uz.company.lunchbot.enums.UserRole.ADMIN
                || user.getRole() == uz.company.lunchbot.enums.UserRole.SUPER_ADMIN;
    }
}
