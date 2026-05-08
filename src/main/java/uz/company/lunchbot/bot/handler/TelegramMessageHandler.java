package uz.company.lunchbot.bot.handler;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMenuLabels;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.dto.request.UpdateMenuItemImageRequest;
import uz.company.lunchbot.dto.request.RecalculateSessionRequest;
import uz.company.lunchbot.dto.request.TelegramRegistrationRequest;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.RestaurantVoteSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.RecalculationMode;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.exception.OrderSessionClosedException;
import uz.company.lunchbot.service.MenuItemService;
import uz.company.lunchbot.service.NotificationService;
import uz.company.lunchbot.service.MenuPhotoUploadStateService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.PaymentService;
import uz.company.lunchbot.service.RestaurantVoteSessionService;
import uz.company.lunchbot.service.SummaryService;
import uz.company.lunchbot.service.TelegramAdminCommandService;
import uz.company.lunchbot.service.TelegramRegistrationStateService;
import uz.company.lunchbot.service.UserOrderService;
import uz.company.lunchbot.service.UserService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMessageHandler {

    private final UserService userService;
    private final OrderSessionService orderSessionService;
    private final UserOrderService userOrderService;
    private final SummaryService summaryService;
    private final NotificationService telegramNotificationService;
    private final TelegramMessages telegramMessages;
    private final TelegramAdminCommandService telegramAdminCommandService;
    private final RestaurantVoteSessionService restaurantVoteSessionService;
    private final PaymentService paymentService;
    private final TelegramRegistrationStateService registrationStateService;
    private final TelegramKeyboards telegramKeyboards;
    private final MenuItemService menuItemService;
    private final MenuPhotoUploadStateService menuPhotoUploadStateService;

    public void handle(Message message) {
        if (!isPrivateChat(message)) {
            return;
        }

        Long chatId = message.getChatId();
        Long telegramUserId = message.getFrom().getId();

        if (message.hasContact()) {
            handleContactMessage(message, chatId, telegramUserId);
            return;
        }

        if (message.hasPhoto() || message.hasDocument()) {
            handleReceiptMessage(message, chatId);
            return;
        }

        if (!message.hasText()) {
            return;
        }

        String text = message.getText().trim();
        Optional<LunchUser> existingUser = userService.findByTelegramUserId(telegramUserId);

        try {
            if (text.startsWith("/start")) {
                handleStart(message, existingUser.orElse(null));
                return;
            }

            LunchUser user = existingUser.orElseThrow(() -> new NotFoundException(telegramMessages.registrationRequired(resolveLanguage(null, telegramUserId))));

            if (!isRegistrationComplete(user)) {
                telegramNotificationService.sendPrivateText(chatId, telegramMessages.chooseLanguage(), telegramKeyboards.languageSelection());
                return;
            }

            UserLanguage language = languageOf(user);
            if (text.equals(TelegramMenuLabels.changeLanguage(language))) {
                telegramNotificationService.sendPrivateText(chatId, telegramMessages.chooseLanguage(), telegramKeyboards.languageSelection());
                return;
            }

            if (text.equals("/status")) {
                sendStatus(chatId, user);
                return;
            }

            if (user.getStatus() != UserStatus.APPROVED) {
                sendStatusAwareMessage(user, chatId);
                return;
            }

            if (text.equals(TelegramMenuLabels.todaysMenu(language))) {
                handleTodaysMenu(chatId, user);
            } else if (text.equals(TelegramMenuLabels.placeOrder(language))) {
                handlePlaceOrder(chatId, user);
            } else if (text.equals(TelegramMenuLabels.myOrder(language))) {
                handleMyOrder(chatId, telegramUserId, language);
            } else if (text.equals(TelegramMenuLabels.skipToday(language))) {
                handleSkipToday(telegramUserId, user);
            } else if (text.equals(TelegramMenuLabels.help(language))) {
                telegramNotificationService.sendPrivateText(chatId, telegramMessages.help(language), null);
            } else {
                handleAdminOrFallback(text, user, chatId);
            }
        } catch (NotFoundException exception) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.registrationRequired(resolveLanguage(existingUser.orElse(null), telegramUserId)), null);
        } catch (Exception exception) {
            log.error("telegram_message_failed chatId={} reason={}", chatId, exception.getMessage(), exception);
            telegramNotificationService.sendPrivateText(chatId, exception.getMessage(), null);
        }
    }

    private void handleReceiptMessage(Message message, Long chatId) {
        try {
            Long telegramUserId = message.getFrom().getId();
            Optional<Long> pendingMenuItemId = menuPhotoUploadStateService.getPendingMenuItemId(telegramUserId);
            if (pendingMenuItemId.isPresent()) {
                handlePendingMenuPhotoUpload(message, chatId, telegramUserId, pendingMenuItemId.get());
                return;
            }

            String fileId = message.hasPhoto()
                    ? message.getPhoto().get(message.getPhoto().size() - 1).getFileId()
                    : message.getDocument().getFileId();
            paymentService.submitReceipt(telegramUserId, fileId, message.getMessageId().longValue());
        } catch (Exception exception) {
            log.error("telegram_receipt_failed chatId={} reason={}", chatId, exception.getMessage(), exception);
            telegramNotificationService.sendPrivateText(chatId, exception.getMessage(), null);
        }
    }

    private void handlePendingMenuPhotoUpload(Message message, Long chatId, Long telegramUserId, Long menuItemId) {
        if (!message.hasPhoto()) {
            telegramNotificationService.sendPrivateText(chatId, "Send photo for menu item #" + menuItemId, null);
            return;
        }

        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        MenuItem menuItem = menuItemService.getRequired(menuItemId);
        String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
        menuItemService.updateImage(
                menuItemId,
                new UpdateMenuItemImageRequest(menuItem.getImageUrl(), fileId),
                actor.getId()
        );
        menuPhotoUploadStateService.clear(telegramUserId);
        telegramNotificationService.sendPrivateText(chatId, "Photo saved for menu item #" + menuItemId, null);
    }

    private void handleStart(Message message, LunchUser existingUser) {
        if (existingUser == null || !isRegistrationComplete(existingUser)) {
            telegramNotificationService.sendPrivateText(message.getChatId(), telegramMessages.chooseLanguage(), telegramKeyboards.languageSelection());
            return;
        }
        if (existingUser.getStatus() == UserStatus.APPROVED && hasStartPayload(message, "order")) {
            handlePlaceOrder(message.getChatId(), existingUser);
            return;
        }
        sendStatusAwareMessage(existingUser, message.getChatId());
    }

    private void handleContactMessage(Message message, Long chatId, Long telegramUserId) {
        try {

            log.info(
                    "telegram_contact_received chatId={} telegramUserId={} contactUserId={} phone={}",
                    chatId,
                    telegramUserId,
                    message.getContact() == null ? null : message.getContact().getUserId(),
                    message.getContact() == null ? null : message.getContact().getPhoneNumber()
            );
            log.info("telegram_contact_received chatId={} telegramUserId={}", chatId, telegramUserId);

            LunchUser existingUser = userService.findByTelegramUserId(telegramUserId).orElse(null);
            Optional<UserLanguage> pendingLanguage = registrationStateService.getLanguage(telegramUserId);

            if (message.getContact() == null) {
                telegramNotificationService.sendPrivateText(
                        chatId,
                        telegramMessages.invalidContact(resolveLanguage(existingUser, telegramUserId)),
                        null
                );
                return;
            }

            Long contactUserId = message.getContact().getUserId();

            if (contactUserId == null || !telegramUserId.equals(contactUserId)) {
                telegramNotificationService.sendPrivateText(
                        chatId,
                        telegramMessages.invalidContact(resolveLanguage(existingUser, telegramUserId)),
                        null
                );
                return;
            }

            UserLanguage language = pendingLanguage.orElse(resolveLanguage(existingUser, telegramUserId));

            LunchUser user = userService.completeTelegramRegistration(
                    new TelegramRegistrationRequest(
                            telegramUserId,
                            message.getFrom().getUserName(),
                            message.getFrom().getFirstName(),
                            message.getFrom().getLastName(),
                            chatId,
                            message.getContact().getPhoneNumber()
                    ),
                    language
            );

            registrationStateService.clear(telegramUserId);

            if (user.getStatus() == UserStatus.APPROVED) {
                telegramNotificationService.sendApprovedMainMenu(
                        user,
                        telegramMessages.approvedMenuGreeting(user)
                );
                return;
            }

            if (user.getStatus() == UserStatus.PENDING) {
                telegramNotificationService.notifyAdminsAboutPendingUser(user);
                telegramNotificationService.sendPrivateText(
                        chatId,
                        telegramMessages.registrationSubmitted(language),
                        telegramKeyboards.removeKeyboard()
                );
                return;
            }

            sendStatusAwareMessage(user, chatId);

        } catch (Exception exception) {
            log.error("telegram_contact_failed chatId={} reason={}", chatId, exception.getMessage(), exception);
            telegramNotificationService.sendPrivateText(chatId, exception.getMessage(), null);
        }
    }

    private void sendStatusAwareMessage(LunchUser user, Long chatId) {
        UserLanguage language = languageOf(user);
        if (user.getStatus() == UserStatus.PENDING) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.welcomePending(language), null);
        } else if (user.getStatus() == UserStatus.REJECTED) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.welcomeRejected(language), null);
        } else if (user.getStatus() == UserStatus.BLOCKED) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.accessDenied(language), null);
        } else {
            telegramNotificationService.sendApprovedMainMenu(user, currentStateGreeting(user));
        }
    }

    private void handleTodaysMenu(Long chatId, LunchUser user) {
        Optional<String> disabledReason = resolveOrderActionDisabledReason(user, PrivateOrderAction.TODAY_MENU);
        if (disabledReason.isPresent()) {
            logPrivateActionBlocked(user, PrivateOrderAction.TODAY_MENU, disabledReason.get());
            telegramNotificationService.sendPrivateText(chatId, disabledReason.get(), null);
            return;
        }

        OrderSession session = orderSessionService.getActiveOrderingSession();
        telegramNotificationService.sendTodayMenu(chatId, session);
    }

    private void handlePlaceOrder(Long chatId, LunchUser user) {
        Optional<String> disabledReason = resolveOrderActionDisabledReason(user, PrivateOrderAction.PLACE_ORDER);
        if (disabledReason.isPresent()) {
            logPrivateActionBlocked(user, PrivateOrderAction.PLACE_ORDER, disabledReason.get());
            telegramNotificationService.sendPrivateText(chatId, disabledReason.get(), null);
            return;
        }

        OrderSession session = orderSessionService.getActiveOrderingSession();
        telegramNotificationService.sendMenuSelection(chatId, session);
    }

    private void handleMyOrder(Long chatId, Long telegramUserId, UserLanguage language) {
        LunchUser user = userService.getApprovedUserByTelegramUserId(telegramUserId);
        Optional<String> disabledReason = resolveOrderActionDisabledReason(user, PrivateOrderAction.MY_ORDER);
        if (disabledReason.isPresent()) {
            logPrivateActionBlocked(user, PrivateOrderAction.MY_ORDER, disabledReason.get());
            telegramNotificationService.sendPrivateText(chatId, disabledReason.get(), null);
            return;
        }

        UserOrder order = userOrderService.getTodayOrderForTelegramUser(telegramUserId).orElse(null);
        telegramNotificationService.sendPrivateText(chatId, order == null ? telegramMessages.noResponseYet(language) : telegramMessages.myOrder(order, language), null);
    }

    private void handleSkipToday(Long telegramUserId, LunchUser user) {
        Optional<String> disabledReason = resolveOrderActionDisabledReason(user, PrivateOrderAction.SKIP_TODAY);
        if (disabledReason.isPresent()) {
            logPrivateActionBlocked(user, PrivateOrderAction.SKIP_TODAY, disabledReason.get());
            telegramNotificationService.sendPrivateText(user.getPrivateChatId(), disabledReason.get(), null);
            return;
        }

        userOrderService.skipToday(telegramUserId);
        telegramNotificationService.sendApprovedMainMenu(user, telegramMessages.skippedToday(languageOf(user)));
    }

    private void handleAdminOrFallback(String text, LunchUser user, Long chatId) {
        UserLanguage language = languageOf(user);
        boolean admin = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN;
        if (!admin) {
            telegramNotificationService.sendApprovedMainMenu(user, telegramMessages.help(language));
            return;
        }

        if (telegramAdminCommandService.supports(text)) {
            var response = telegramAdminCommandService.handle(user, text);
            telegramNotificationService.sendPrivateText(chatId, response.text(), response.keyboard());
            return;
        }

        if (text.equals(TelegramMenuLabels.todaysSummary(language))) {
            sendTodaySummary(chatId, language);
        } else if (text.equals(TelegramMenuLabels.notRespondedUsers(language))) {
            sendNotRespondedUsers(chatId, language);
        } else if (text.equals(TelegramMenuLabels.pendingUsers(language))) {
            telegramNotificationService.sendPendingUsersList(chatId, userService.getPendingUsers());
        } else if (text.equals(TelegramMenuLabels.closeOrder(language))) {
            closeTodayOrder(user, chatId);
        } else if (text.equals(TelegramMenuLabels.confirmOrder(language))) {
            confirmTodayOrder(user, chatId);
        } else if (text.equals(TelegramMenuLabels.extendDeadline(language))) {
            extendTodayOrder(user, chatId);
        } else if (text.equals(TelegramMenuLabels.paymentSummary(language))) {
            sendPaymentSummary(user, chatId);
        } else if (text.equals(TelegramMenuLabels.manageRestaurants(language))) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.restaurantManagementHelp(), null);
        } else if (text.equals(TelegramMenuLabels.manageMenu(language))) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.menuManagementHelp(), null);
        } else if (text.equals(TelegramMenuLabels.setCurrentSessionDeliveryPrice(language))) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.sessionPricingHelp(), null);
        } else if (text.equals(TelegramMenuLabels.recalculateCurrentSession(language))) {
            recalculateCurrentSession(user, chatId);
        } else if (text.equals(TelegramMenuLabels.manualOrderEdit(language))) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.featureHandledViaApi("Manual Order Edit"), null);
        } else {
            telegramNotificationService.sendApprovedMainMenu(user, telegramMessages.help(language));
        }
    }

    private void sendTodaySummary(Long chatId, UserLanguage language) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday(language)));
        SessionSummaryResponse summary = summaryService.buildSummary(session.getId());
        telegramNotificationService.sendPrivateText(chatId, summary.groupSummaryText(), null);
    }

    private void sendNotRespondedUsers(Long chatId, UserLanguage language) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday(language)));
        List<LunchUser> users = userOrderService.findNotRespondedUsers(session);
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.notRespondedReminder(session, users), null);
    }

    private void closeTodayOrder(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodayOpenSession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday(languageOf(actor))));
        OrderSession closed = orderSessionService.closeSession(session.getId(), actor.getId());
        SessionSummaryResponse summary = summaryService.buildSummary(closed.getId());
        telegramNotificationService.sendClosedSummary(summary, closed.getId());
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.actionCompleted("Close order"), null);
    }

    private void confirmTodayOrder(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday(languageOf(actor))));
        OrderSession confirmed = orderSessionService.confirmSession(session.getId(), actor.getId());
        telegramNotificationService.sendConfirmedSummary(summaryService.buildSummary(confirmed.getId()));
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.actionCompleted("Confirm order"), null);
    }

    private void extendTodayOrder(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday(languageOf(actor))));
        OrderSession extended = orderSessionService.extendSession(session.getId(), 10, actor.getId());
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.sessionExtended(extended), null);
    }

    private void recalculateCurrentSession(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday(languageOf(actor))));
        RecalculateSessionRequest request = new RecalculateSessionRequest(RecalculationMode.FULL_PRICE_REBUILD, false);
        orderSessionService.recalculateSession(session.getId(), request, actor.getId());
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.actionCompleted("Current session recalculated"), null);
    }

    private void sendPaymentSummary(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday(languageOf(actor))));
        telegramNotificationService.sendPrivateText(chatId, paymentService.buildPaymentSummaryText(session.getId(), actor.getId()), null);
    }

    private void sendStatus(Long chatId, LunchUser user) {
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
            var response = telegramAdminCommandService.handle(user, "/status");
            telegramNotificationService.sendPrivateText(chatId, response.text(), response.keyboard());
            return;
        }

        UserLanguage language = languageOf(user);
        RestaurantVoteSession voteSession = restaurantVoteSessionService.getTodaySession().orElse(null);
        if (voteSession != null && voteSession.getStatus() == RestaurantVoteSessionStatus.OPEN) {
            telegramNotificationService.sendPrivateText(chatId, (language == UserLanguage.RU ? "⏳ Сейчас идет голосование за ресторан. Дедлайн: " : "⏳ Hozir restoran voting davom etmoqda. Deadline: ") + voteSession.getDeadlineAt().toLocalTime(), null);
            return;
        }

        OrderSession openSession = orderSessionService.getTodayOpenSession().orElse(null);
        if (openSession != null && openSession.getDeadlineAt().isAfter(java.time.LocalDateTime.now())) {
            telegramNotificationService.sendPrivateText(
                    chatId,
                    (language == UserLanguage.RU
                            ? "🍽 Заказ открыт.\nРесторан: " + openSession.getRestaurant().getName() + "\nDeadline: " + openSession.getDeadlineAt().toLocalTime()
                            : "🍽 Buyurtma ochiq.\nRestoran: " + openSession.getRestaurant().getName() + "\nDeadline: " + openSession.getDeadlineAt().toLocalTime()),
                    null
            );
            return;
        }

        telegramNotificationService.sendPrivateText(chatId, language == UserLanguage.RU ? "🔒 Сегодняшний заказ закрыт." : "🔒 Bugungi buyurtma yopilgan.", null);
    }

    private String resolveMissingSessionMessage(UserLanguage language) {
        RestaurantVoteSession voteSession = restaurantVoteSessionService.getTodaySession().orElse(null);
        if (voteSession == null) {
            return telegramMessages.orderBlockedNoSession(language);
        }
        if (voteSession.getStatus() == RestaurantVoteSessionStatus.OPEN) {
            return telegramMessages.orderBlockedVotingOpen(language);
        }
        if (voteSession.getWinnerRestaurant() == null || voteSession.getStatus() == RestaurantVoteSessionStatus.TIE_WAITING_ADMIN) {
            return telegramMessages.orderBlockedWinnerNotSelected(language);
        }
        return telegramMessages.orderBlockedNoSession(language);
    }

    private String currentStateGreeting(LunchUser user) {
        UserLanguage language = languageOf(user);
        RestaurantVoteSession voteSession = restaurantVoteSessionService.getTodaySession().orElse(null);
        if (voteSession != null && voteSession.getStatus() == RestaurantVoteSessionStatus.OPEN) {
            return telegramMessages.orderBlockedVotingOpen(language);
        }
        if (voteSession != null && (voteSession.getStatus() == RestaurantVoteSessionStatus.TIE_WAITING_ADMIN || voteSession.getWinnerRestaurant() == null)) {
            return telegramMessages.orderBlockedWinnerNotSelected(language);
        }

        OrderSession openSession = orderSessionService.getTodayOpenSession().orElse(null);
        if (openSession != null && openSession.getDeadlineAt().isAfter(java.time.LocalDateTime.now())) {
            return language == UserLanguage.RU
                    ? "🍽 Сегодняшний заказ открыт. Дедлайн: " + openSession.getDeadlineAt().toLocalTime()
                    : "🍽 Bugungi buyurtma ochiq. Deadline: " + openSession.getDeadlineAt().toLocalTime();
        }

        return telegramMessages.orderBlockedClosed(language);
    }

    private Optional<String> resolveOrderActionDisabledReason(LunchUser user, PrivateOrderAction action) {
        UserLanguage language = languageOf(user);
        RestaurantVoteSession voteSession = restaurantVoteSessionService.getTodaySession().orElse(null);
        if (voteSession != null) {
            if (voteSession.getStatus() == RestaurantVoteSessionStatus.OPEN) {
                return Optional.of(telegramMessages.orderBlockedVotingOpen(language));
            }
            if (voteSession.getStatus() == RestaurantVoteSessionStatus.TIE_WAITING_ADMIN || voteSession.getWinnerRestaurant() == null) {
                return Optional.of(telegramMessages.orderBlockedWinnerNotSelected(language));
            }
        }

        OrderSession session = orderSessionService.getTodaySession().orElse(null);
        if (session == null) {
            return Optional.of(telegramMessages.orderBlockedNoSession(language));
        }
        if (session.getStatus() != uz.company.lunchbot.enums.OrderSessionStatus.OPEN) {
            return Optional.of(telegramMessages.orderBlockedClosed(language));
        }

        try {
            orderSessionService.ensureUserCanPlaceOrder(session);
        } catch (OrderSessionClosedException exception) {
            return Optional.of(telegramMessages.orderBlockedDeadlinePassed(language));
        }

        return Optional.empty();
    }

    private void logPrivateActionBlocked(LunchUser user, PrivateOrderAction action, String reason) {
        log.info(
                "private_order_action_blocked userId={} telegramUserId={} action={} reason={}",
                user.getId(),
                user.getTelegramUserId(),
                action,
                reason
        );
    }

    private UserLanguage languageOf(LunchUser user) {
        return user == null || user.getLanguage() == null ? UserLanguage.UZ : user.getLanguage();
    }

    private UserLanguage resolveLanguage(LunchUser user, Long telegramUserId) {
        if (user != null && user.getLanguage() != null) {
            return user.getLanguage();
        }
        return registrationStateService.getLanguage(telegramUserId).orElse(UserLanguage.UZ);
    }

    private boolean isRegistrationComplete(LunchUser user) {
        return user != null
                && user.getPrivateChatId() != null
                && user.getPhoneNumber() != null
                && user.getLanguage() != null;
    }

    private boolean hasStartPayload(Message message, String payload) {
        if (message == null || !message.hasText()) {
            return false;
        }

        String[] parts = message.getText().trim().split("\\s+", 2);
        return parts.length == 2 && "/start".equals(parts[0]) && payload.equalsIgnoreCase(parts[1].trim());
    }

    private boolean isPrivateChat(Message message) {
        return message.getChat() != null && "private".equalsIgnoreCase(message.getChat().getType());
    }

    private enum PrivateOrderAction {
        TODAY_MENU,
        PLACE_ORDER,
        MY_ORDER,
        SKIP_TODAY
    }
}
