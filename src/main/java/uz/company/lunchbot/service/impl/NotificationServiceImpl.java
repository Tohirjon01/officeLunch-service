package uz.company.lunchbot.service.impl;

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
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.exception.TelegramMessageException;
import uz.company.lunchbot.service.MenuItemService;
import uz.company.lunchbot.service.NotificationService;
import uz.company.lunchbot.service.UserService;
import uz.company.lunchbot.service.calculation.ContainerPricingService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MENU_PAGE_SIZE = 10;

    private final TelegramApiClient telegramApiClient;
    private final TelegramMessages telegramMessages;
    private final TelegramKeyboards telegramKeyboards;
    private final TelegramBotProperties telegramBotProperties;
    private final LunchProperties lunchProperties;
    private final MenuItemService menuItemService;
    private final UserService userService;
    private final ContainerPricingService containerPricingService;

    @Override
    public void sendPrivateText(Long chatId, String text, Object keyboard) {
        if (chatId == null) {
            return;
        }
        telegramApiClient.sendMessage(chatId, text, keyboard);
    }

    @Override
    public Long sendGroupText(String text, Object replyMarkup) {
        Long groupChatId = requireGroupChatId();

        log.info("telegram_group_send chatId={}", groupChatId);

        return telegramApiClient.sendMessageAndGetMessageId(groupChatId, text, replyMarkup);
    }
    @Override
    public void editGroupText(Long messageId, String text, Object keyboard) {
        if (messageId == null) {
            return;
        }
        telegramApiClient.editMessageText(requireGroupChatId(), messageId, text, keyboard);
    }

    @Override
    public void editText(Long chatId, Long messageId, String text, Object keyboard) {
        if (chatId == null || messageId == null) {
            return;
        }
        telegramApiClient.editMessageText(chatId, messageId, text, keyboard);
    }

    @Override
    public void copyPrivateMessage(Long chatId, Long sourceChatId, Long sourceMessageId) {
        if (chatId == null || sourceChatId == null || sourceMessageId == null) {
            return;
        }
        telegramApiClient.copyMessage(chatId, sourceChatId, sourceMessageId);
    }

    @Override
    public void answerCallback(String callbackQueryId, String text) {
        try {
            telegramApiClient.answerCallbackQuery(callbackQueryId, text);
        } catch (TelegramMessageException exception) {
            log.warn(
                    "telegram_answer_callback_failed callbackQueryId={} reason={}",
                    callbackQueryId,
                    exception.getMessage()
            );
        } catch (Exception exception) {
            log.warn(
                    "telegram_answer_callback_unexpected_failed callbackQueryId={} reason={}",
                    callbackQueryId,
                    exception.getMessage()
            );
        }
    }

    @Override
    public void sendApprovedMainMenu(LunchUser user, String text) {
        sendPrivateText(user.getPrivateChatId(), text, telegramKeyboards.mainMenu(languageOf(user), isAdmin(user)));
    }

    @Override
    public void sendTodayMenu(Long chatId, OrderSession session) {
        List<MenuItem> menuItems = menuItemService.getActiveMenu(session.getRestaurant().getId());
        sendPrivateText(chatId, telegramMessages.todayMenu(session, menuItems, languageForChat(chatId)), null);
    }

    @Override
    public void sendMenuSelection(Long chatId, OrderSession session) {
        UserLanguage language = languageForChat(chatId);
        List<MenuItem> menuItems = menuItemService.getActiveMenu(session.getRestaurant().getId());
        List<String> categories = categoryKeys(menuItems);
        List<String> categoryLabels = categories.stream().map(category -> telegramMessages.categoryLabel(category, language)).toList();
        sendPrivateText(chatId, telegramMessages.chooseMenuCategory(session, categoryLabels, language), telegramKeyboards.menuSelection(session, categoryLabels, language));
    }

    @Override
    public void sendMenuCategorySelection(Long chatId, OrderSession session, int categoryIndex, int page) {
        UserLanguage language = languageForChat(chatId);
        List<MenuItem> menuItems = menuItemService.getActiveMenu(session.getRestaurant().getId());
        List<String> categories = categoryKeys(menuItems);
        if (categories.isEmpty()) {
            sendPrivateText(chatId, telegramMessages.chooseMenuItemFromCategory(session, telegramMessages.categoryLabel(null, language), List.of(), 0, 1, language), null);
            return;
        }

        int boundedCategoryIndex = Math.max(0, Math.min(categoryIndex, categories.size() - 1));
        String category = categories.get(boundedCategoryIndex);
        List<MenuItem> itemsInCategory = menuItems.stream().filter(item -> java.util.Objects.equals(item.getCategory(), category)).toList();
        int totalPages = Math.max(1, (itemsInCategory.size() + MENU_PAGE_SIZE - 1) / MENU_PAGE_SIZE);
        int boundedPage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(boundedPage * MENU_PAGE_SIZE, itemsInCategory.size());
        int toIndex = Math.min(fromIndex + MENU_PAGE_SIZE, itemsInCategory.size());
        List<MenuItem> pageItems = itemsInCategory.subList(fromIndex, toIndex);

        sendPrivateText(
                chatId,
                telegramMessages.chooseMenuItemFromCategory(session, telegramMessages.categoryLabel(category, language), pageItems, boundedPage, totalPages, language),
                telegramKeyboards.menuCategoryItems(session, boundedCategoryIndex, boundedPage, totalPages, pageItems, language)
        );
    }

    @Override
    public void sendMenuItemPreview(Long chatId, OrderSession session, MenuItem menuItem) {
        UserLanguage language = languageForChat(chatId);
        String caption = telegramMessages.menuItemPreview(menuItem, containerPricingService.resolve(menuItem), language);
        Object keyboard = telegramKeyboards.menuItemPreview(session.getId(), menuItem.getId(), language);

        String photo = menuItem.getTelegramImageFileId();
        boolean attemptedPhoto = false;
        boolean sent = false;

        if (photo != null) {
            attemptedPhoto = true;
            sent = telegramApiClient.sendPhoto(chatId, photo, caption, keyboard);
        }
        if (!sent && menuItem.getImageUrl() != null) {
            attemptedPhoto = true;
            sent = telegramApiClient.sendPhoto(chatId, menuItem.getImageUrl(), caption, keyboard);
        }
        if (sent) {
            return;
        }

        if (attemptedPhoto) {
            log.warn("menu_item_preview_photo_fallback menuItemId={} chatId={}", menuItem.getId(), chatId);
        }

        sendPrivateText(chatId, caption, keyboard);
    }

    @Override
    public void notifyAdminsAboutPendingUser(LunchUser pendingUser) {
        userService.getApprovedAdmins().stream()
                .filter(admin -> admin.getPrivateChatId() != null)
                .forEach(admin -> sendPrivateText(
                        admin.getPrivateChatId(),
                        telegramMessages.pendingUserApprovalRequest(pendingUser, languageOf(admin)),
                        telegramKeyboards.pendingUserActions(pendingUser.getId())));
    }

    @Override
    public void sendGroupOpenAnnouncement(OrderSession session) {
        telegramApiClient.sendMessage(
                requireGroupChatId(),
                telegramMessages.groupOpenAnnouncement(session),
                telegramKeyboards.groupPlaceOrderButton(telegramBotProperties.username()));
    }

    @Override
    public void sendReminder(OrderSession session, List<LunchUser> notRespondedUsers) {
        telegramApiClient.sendMessage(requireGroupChatId(), telegramMessages.orderClosingReminder(), null);
        notRespondedUsers.stream()
                .filter(user -> user.getPrivateChatId() != null)
                .forEach(user -> sendPrivateText(user.getPrivateChatId(), telegramMessages.privateReminder(session), null));
    }

    @Override
    public void sendClosedSummary(SessionSummaryResponse summary, Long sessionId) {
        telegramApiClient.sendMessage(
                requireGroupChatId(),
                telegramMessages.sessionClosedShort(summary.orderedCount(), summary.skippedCount(), summary.noResponseCount()),
                telegramKeyboards.adminSessionActions(sessionId));
        telegramApiClient.sendMessage(requireGroupChatId(), summary.groupSummaryText(), null);
    }

    @Override
    public void sendConfirmedSummary(SessionSummaryResponse summary) {
        telegramApiClient.sendMessage(requireGroupChatId(), telegramMessages.sessionConfirmed(), null);
        telegramApiClient.sendMessage(requireGroupChatId(), summary.restaurantOrderText(), null);
    }

    @Override
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

    private UserLanguage languageOf(LunchUser user) {
        return user == null || user.getLanguage() == null ? UserLanguage.UZ : user.getLanguage();
    }

    private UserLanguage languageForChat(Long chatId) {
        return userService.findByPrivateChatId(chatId)
                .map(this::languageOf)
                .orElse(UserLanguage.UZ);
    }

    private Long requireGroupChatId() {
        Long groupChatId = lunchProperties.groupChatId();
        if (groupChatId == null) {
            throw new IllegalStateException("lunch.group-chat-id must be configured");
        }
        return groupChatId;
    }

    private List<String> categoryKeys(List<MenuItem> menuItems) {
        return menuItems.stream()
                .map(MenuItem::getCategory)
                .distinct()
                .sorted(java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
