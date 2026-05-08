package uz.company.lunchbot.bot.handler;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.dto.response.RestaurantVoteSessionResponse;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.ForbiddenException;
import uz.company.lunchbot.exception.NotFoundException;
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
import uz.company.lunchbot.util.CallbackDataParser.ParsedCallback;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramCallbackHandler {

    private final CallbackDataParser callbackDataParser;
    private final UserService userService;
    private final UserOrderService userOrderService;
    private final OrderSessionService orderSessionService;
    private final MenuItemService menuItemService;
    private final SummaryService summaryService;
    private final NotificationService telegramNotificationService;
    private final TelegramMessages telegramMessages;
    private final RestaurantVoteSessionService restaurantVoteSessionService;
    private final PaymentService paymentService;
    private final TelegramAdminCommandService telegramAdminCommandService;
    private final TelegramRegistrationStateService registrationStateService;
    private final TelegramKeyboards telegramKeyboards;

    private static final String ADMIN_PAYMENT_APPROVE_PREFIX = "admin_payment_approve_";
    private static final String ADMIN_PAYMENT_REJECT_PREFIX = "admin_payment_reject_";

    public void handle(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long telegramUserId = callbackQuery.getFrom().getId();

        try {
            if (handleAdminPaymentCallback(callbackData, telegramUserId, callbackQuery.getId())) {
                return;
            }

            ParsedCallback parsed = callbackDataParser.parse(callbackData);
            switch (parsed.action()) {
                case "LANG" -> handleLanguageSelection(chatId, telegramUserId, parsed.args().getFirst(), callbackQuery.getId());
                case "ORDER_ITEM" -> handleUserOrder(chatId, telegramUserId, Long.parseLong(parsed.args().get(0)), Long.parseLong(parsed.args().get(1)), callbackQuery.getId());
                case "MENU_PAGE" -> handleMenuPage(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().get(0)), Integer.parseInt(parsed.args().get(1)), callbackQuery.getId());
                case "MENU_CATEGORY" -> handleMenuCategory(chatId, telegramUserId, Long.parseLong(parsed.args().get(0)), Integer.parseInt(parsed.args().get(1)), Integer.parseInt(parsed.args().get(2)), callbackQuery.getId());
                case "MENU_ITEM_PREVIEW" -> handleMenuItemPreview(chatId, telegramUserId, Long.parseLong(parsed.args().get(0)), Long.parseLong(parsed.args().get(1)), callbackQuery.getId());
                case "MENU_SELECTION" -> handleMenuSelection(chatId, telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "USER_ORDER_MENU" -> handleUserOrder(chatId, telegramUserId, null, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "USER_ORDER_SKIP" -> handleUserSkip(chatId, telegramUserId, callbackQuery.getId());
                case "ADMIN_CONFIRM_SESSION" -> handleAdminConfirm(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_CANCEL_SESSION" -> handleAdminCancel(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_EXTEND_SESSION" -> handleAdminExtend(telegramUserId, Long.parseLong(parsed.args().get(0)), Integer.parseInt(parsed.args().get(1)), callbackQuery.getId());
                case "ORDER_EXTEND" -> handleOrderExtend(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().get(0)), Integer.parseInt(parsed.args().get(1)), callbackQuery.getId());
                case "ORDER_REDUCE" -> handleOrderReduce(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().get(0)), Integer.parseInt(parsed.args().get(1)), callbackQuery.getId());
                case "ORDER_CLOSE_NOW" -> handleOrderCloseNow(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ORDER_SUMMARY" -> handleOrderSummary(chatId, telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ORDER_CONTROL_REFRESH" -> handleOrderControlRefresh(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "VOTE_EXTEND" -> handleVoteExtend(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().get(0)), Integer.parseInt(parsed.args().get(1)), callbackQuery.getId());
                case "VOTE_REDUCE" -> handleVoteReduce(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().get(0)), Integer.parseInt(parsed.args().get(1)), callbackQuery.getId());
                case "VOTE_CLOSE_NOW" -> handleVoteCloseNow(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "VOTE_RESULT" -> handleVoteResult(chatId, telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "VOTE_CONTROL_REFRESH" -> handleVoteControlRefresh(chatId, callbackQuery.getMessage().getMessageId().longValue(), telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "APPROVE_USER", "ADMIN_APPROVE_USER" -> handleAdminApproveUser(chatId, telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "REJECT_USER", "ADMIN_REJECT_USER" -> handleAdminRejectUser(chatId, telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_SUMMARY" -> handleAdminSummary(chatId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_CLOSE_SESSION" -> handleAdminClose(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "RESTAURANT_VOTE" -> handleRestaurantVote(telegramUserId, Long.parseLong(parsed.args().get(0)), Long.parseLong(parsed.args().get(1)), callbackQuery.getId());
                case "RESTAURANT_WINNER" -> handleRestaurantWinner(telegramUserId, Long.parseLong(parsed.args().get(0)), Long.parseLong(parsed.args().get(1)), callbackQuery.getId());
                case "PAYMENT_CASH", "PAYMENT_DECLARE_CASH" -> handleDeclareCash(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "PAYMENT_UPLOAD_RECEIPT" -> handleUploadReceiptPrompt(chatId, telegramUserId, callbackQuery.getId());
                case "PAYMENT_STATUS" -> handlePaymentStatus(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "PAYMENT_APPROVE", "ADMIN_PAYMENT_APPROVE" -> handleApprovePayment(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "PAYMENT_REJECT", "ADMIN_PAYMENT_REJECT" -> handleRejectPayment(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "PAYMENT_MARK_CASH_PAID" -> handleMarkCashPaid(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                default -> telegramNotificationService.answerCallback(callbackQuery.getId(), "Unknown action");
            }
        } catch (Exception exception) {
            log.error("telegram_callback_failed data={} reason={}", callbackQuery.getData(), exception.getMessage(), exception);
            telegramNotificationService.answerCallback(callbackQuery.getId(), exception.getMessage());
        }
    }

    private boolean handleAdminPaymentCallback(String callbackData, Long telegramUserId, String callbackQueryId) {
        if (callbackData == null) {
            return false;
        }
        if (callbackData.startsWith(ADMIN_PAYMENT_APPROVE_PREFIX)) {
            handleApprovePayment(telegramUserId, Long.parseLong(callbackData.substring(ADMIN_PAYMENT_APPROVE_PREFIX.length())), callbackQueryId);
            return true;
        }
        if (callbackData.startsWith(ADMIN_PAYMENT_REJECT_PREFIX)) {
            handleRejectPayment(telegramUserId, Long.parseLong(callbackData.substring(ADMIN_PAYMENT_REJECT_PREFIX.length())), callbackQueryId);
            return true;
        }
        return false;
    }

    private void handleLanguageSelection(Long chatId, Long telegramUserId, String languageCode, String callbackQueryId) {
        UserLanguage language = UserLanguage.fromCode(languageCode);
        Optional<LunchUser> existing = userService.findByTelegramUserId(telegramUserId);
        if (existing.isPresent() && isRegistrationComplete(existing.get()) && existing.get().getStatus() == uz.company.lunchbot.enums.UserStatus.APPROVED) {
            LunchUser updated = userService.updateLanguage(telegramUserId, language);
            telegramNotificationService.answerCallback(callbackQueryId, telegramMessages.languageUpdated(language));
            telegramNotificationService.sendApprovedMainMenu(updated, telegramMessages.approvedMenuGreeting(updated));
            return;
        }

        if (existing.isPresent()) {
            userService.updateLanguage(telegramUserId, language);
        }
        registrationStateService.rememberLanguage(telegramUserId, language);
        telegramNotificationService.answerCallback(callbackQueryId, telegramMessages.languageUpdated(language));
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.requestPhone(language), telegramKeyboards.contactRequest(language));
    }

    private void handleUserOrder(Long chatId, Long telegramUserId, Long sessionId, Long menuItemId, String callbackQueryId) {
        UserLanguage language = resolveLanguage(telegramUserId);
        OrderSession activeSession = orderSessionService.getActiveOrderingSession();
        if (sessionId != null && !activeSession.getId().equals(sessionId)) {
            throw new NotFoundException(telegramMessages.sessionClosed(language));
        }

        String oldMeal = userOrderService.getTodayOrderForTelegramUser(telegramUserId)
                .filter(order -> order.getMenuItem() != null)
                .map(order -> order.getMenuItem().getName())
                .orElse(null);

        UserOrder order = userOrderService.placeTodayOrder(telegramUserId, menuItemId);
        telegramNotificationService.answerCallback(callbackQueryId, language == UserLanguage.RU ? "Заказ сохранён." : "Buyurtmangiz saqlandi.");
        String message = oldMeal == null
                ? telegramMessages.orderAccepted(order.getMenuItem(), language)
                : telegramMessages.orderUpdated(oldMeal, order.getMenuItem().getName(), language);
        telegramNotificationService.sendPrivateText(chatId, message, null);
    }

    private void handleMenuItemPreview(Long chatId, Long telegramUserId, Long sessionId, Long menuItemId, String callbackQueryId) {
        UserLanguage language = resolveLanguage(telegramUserId);
        OrderSession activeSession = orderSessionService.getActiveOrderingSession();
        if (!activeSession.getId().equals(sessionId)) {
            throw new NotFoundException(telegramMessages.sessionClosed(language));
        }

        MenuItem menuItem = menuItemService.getRequired(menuItemId);
        if (!menuItem.isActive()) {
            throw new NotFoundException(telegramMessages.sessionClosed(language));
        }
        if (!menuItem.getRestaurant().getId().equals(activeSession.getRestaurant().getId())) {
            throw new NotFoundException(telegramMessages.sessionClosed(language));
        }

        telegramNotificationService.answerCallback(callbackQueryId, language == UserLanguage.RU ? "Открыто" : "Ko'rsatildi.");
        telegramNotificationService.sendMenuItemPreview(chatId, activeSession, menuItem);
    }

    private void handleMenuCategory(Long chatId, Long telegramUserId, Long sessionId, int categoryIndex, int page, String callbackQueryId) {
        UserLanguage language = resolveLanguage(telegramUserId);
        OrderSession activeSession = orderSessionService.getActiveOrderingSession();
        if (!activeSession.getId().equals(sessionId)) {
            throw new NotFoundException(telegramMessages.sessionClosed(language));
        }

        telegramNotificationService.answerCallback(callbackQueryId, language == UserLanguage.RU ? "Категория открыта." : "Kategoriya ochildi.");
        telegramNotificationService.sendMenuCategorySelection(chatId, activeSession, categoryIndex, page);
    }

    private void handleMenuSelection(Long chatId, Long telegramUserId, Long sessionId, String callbackQueryId) {
        UserLanguage language = resolveLanguage(telegramUserId);
        OrderSession activeSession = orderSessionService.getActiveOrderingSession();
        if (!activeSession.getId().equals(sessionId)) {
            throw new NotFoundException(telegramMessages.sessionClosed(language));
        }

        telegramNotificationService.answerCallback(callbackQueryId, language == UserLanguage.RU ? "Назад к меню." : "Menyuga qaytildi.");
        telegramNotificationService.sendMenuSelection(chatId, activeSession);
    }

    private void handleMenuPage(Long chatId, Long messageId, Long telegramUserId, Long restaurantId, int page, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        var response = telegramAdminCommandService.buildMenuPage(actor, restaurantId, page);
        telegramNotificationService.editText(chatId, messageId, response.text(), response.keyboard());
        telegramNotificationService.answerCallback(callbackQueryId, "Page updated");
    }

    private void handleUserSkip(Long chatId, Long telegramUserId, String callbackQueryId) {
        UserLanguage language = resolveLanguage(telegramUserId);
        userOrderService.skipToday(telegramUserId);
        telegramNotificationService.answerCallback(callbackQueryId, language == UserLanguage.RU ? "Пропуск сохранён." : "Bugun olmayman saqlandi.");
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.skippedToday(language), null);
    }

    private void handleAdminConfirm(Long telegramUserId, Long sessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        OrderSession session = orderSessionService.confirmSession(sessionId, actor.getId());
        telegramNotificationService.sendConfirmedSummary(summaryService.buildSummary(session.getId()));
        telegramNotificationService.answerCallback(callbackQueryId, "Session confirmed");
    }

    private void handleAdminCancel(Long telegramUserId, Long sessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        orderSessionService.cancelSession(sessionId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "Session cancelled");
    }

    private void handleAdminExtend(Long telegramUserId, Long sessionId, int minutes, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        OrderSession session = orderSessionService.extendSession(sessionId, minutes, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "Session extended");
        telegramNotificationService.sendPrivateText(actor.getPrivateChatId(), telegramMessages.sessionExtended(session), null);
    }

    private void handleOrderExtend(Long chatId, Long messageId, Long telegramUserId, Long sessionId, int minutes, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        OrderSession session = orderSessionService.getRequired(sessionId);
        if (session.getStatus() != OrderSessionStatus.OPEN) {
            telegramNotificationService.answerCallback(callbackQueryId, "Bu order allaqachon yopilgan.");
            refreshOrderControl(chatId, messageId, actor, sessionId);
            return;
        }

        orderSessionService.extendSession(sessionId, minutes, actor.getId());
        refreshOrderControl(chatId, messageId, actor, sessionId);
        telegramNotificationService.answerCallback(callbackQueryId, "Deadline uzaytirildi.");
    }

    private void handleOrderReduce(Long chatId, Long messageId, Long telegramUserId, Long sessionId, int minutes, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        try {
            orderSessionService.reduceDeadline(sessionId, minutes, actor.getId());
            refreshOrderControl(chatId, messageId, actor, sessionId);
            telegramNotificationService.answerCallback(callbackQueryId, "Deadline qisqartirildi.");
        } catch (BadRequestException exception) {
            telegramNotificationService.answerCallback(callbackQueryId, exception.getMessage());
            refreshOrderControl(chatId, messageId, actor, sessionId);
        }
    }

    private void handleOrderCloseNow(Long chatId, Long messageId, Long telegramUserId, Long sessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        OrderSession session = orderSessionService.getRequired(sessionId);
        if (session.getStatus() != OrderSessionStatus.OPEN) {
            telegramNotificationService.answerCallback(callbackQueryId, "Bu order allaqachon yopilgan.");
            refreshOrderControl(chatId, messageId, actor, sessionId);
            return;
        }

        OrderSession closed = orderSessionService.closeSession(sessionId, actor.getId());
        telegramNotificationService.sendClosedSummary(summaryService.buildSummary(closed.getId()), closed.getId());
        log.info("order_closed_manually sessionId={} actorUserId={}", sessionId, actor.getId());
        refreshOrderControl(chatId, messageId, actor, sessionId);
        telegramNotificationService.answerCallback(callbackQueryId, "Order yopildi.");
    }

    private void handleOrderSummary(Long chatId, Long telegramUserId, Long sessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        SessionSummaryResponse summary = summaryService.buildSummary(sessionId);
        telegramNotificationService.sendPrivateText(chatId, summary.groupSummaryText(), null);
        telegramNotificationService.answerCallback(callbackQueryId, "Summary tayyor.");
    }

    private void handleOrderControlRefresh(Long chatId, Long messageId, Long telegramUserId, Long sessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        refreshOrderControl(chatId, messageId, actor, sessionId);
        telegramNotificationService.answerCallback(callbackQueryId, "Yangilandi.");
    }

    private void handleVoteExtend(Long chatId, Long messageId, Long telegramUserId, Long voteSessionId, int minutes, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        try {
            restaurantVoteSessionService.extendDeadline(voteSessionId, minutes, actor.getId());
            refreshVoteControl(chatId, messageId, actor, voteSessionId);
            telegramNotificationService.answerCallback(callbackQueryId, "Voting deadline uzaytirildi.");
        } catch (BadRequestException exception) {
            telegramNotificationService.answerCallback(callbackQueryId, exception.getMessage());
            refreshVoteControl(chatId, messageId, actor, voteSessionId);
        }
    }

    private void handleVoteReduce(Long chatId, Long messageId, Long telegramUserId, Long voteSessionId, int minutes, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        try {
            restaurantVoteSessionService.reduceDeadline(voteSessionId, minutes, actor.getId());
            refreshVoteControl(chatId, messageId, actor, voteSessionId);
            telegramNotificationService.answerCallback(callbackQueryId, "Voting deadline qisqartirildi.");
        } catch (BadRequestException exception) {
            telegramNotificationService.answerCallback(callbackQueryId, exception.getMessage());
            refreshVoteControl(chatId, messageId, actor, voteSessionId);
        }
    }

    private void handleVoteCloseNow(Long chatId, Long messageId, Long telegramUserId, Long voteSessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        RestaurantVoteSessionResponse response = restaurantVoteSessionService.closeSession(voteSessionId, actor.getId());
        refreshVoteControl(chatId, messageId, actor, voteSessionId);
        telegramNotificationService.answerCallback(callbackQueryId, response.status() == uz.company.lunchbot.enums.RestaurantVoteSessionStatus.TIE_WAITING_ADMIN ? "Voting tie. Winner tanlang." : "Voting yopildi.");
    }

    private void handleVoteResult(Long chatId, Long telegramUserId, Long voteSessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        var response = telegramAdminCommandService.buildVoteControl(actor, voteSessionId);
        telegramNotificationService.sendPrivateText(chatId, response.text(), response.keyboard());
        telegramNotificationService.answerCallback(callbackQueryId, "Natija tayyor.");
    }

    private void handleVoteControlRefresh(Long chatId, Long messageId, Long telegramUserId, Long voteSessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        refreshVoteControl(chatId, messageId, actor, voteSessionId);
        telegramNotificationService.answerCallback(callbackQueryId, "Yangilandi.");
    }

    private void refreshOrderControl(Long chatId, Long messageId, LunchUser actor, Long sessionId) {
        var response = telegramAdminCommandService.buildOrderControl(actor, sessionId);
        telegramNotificationService.editText(chatId, messageId, response.text(), response.keyboard());
    }

    private void refreshVoteControl(Long chatId, Long messageId, LunchUser actor, Long voteSessionId) {
        var response = telegramAdminCommandService.buildVoteControl(actor, voteSessionId);
        telegramNotificationService.editText(chatId, messageId, response.text(), response.keyboard());
    }

    private void handleAdminApproveUser(Long chatId, Long telegramUserId, Long targetUserId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        LunchUser approved = userService.approve(targetUserId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "User approved");
        telegramNotificationService.sendPrivateText(chatId, "Approved: " + approved.getDisplayName(), null);
        telegramNotificationService.sendApprovedMainMenu(approved, telegramMessages.approved(approved.getLanguage() == null ? UserLanguage.UZ : approved.getLanguage()));
    }

    private void handleAdminRejectUser(Long chatId, Long telegramUserId, Long targetUserId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        LunchUser rejected = userService.reject(targetUserId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "User rejected");
        telegramNotificationService.sendPrivateText(chatId, "Rejected: " + rejected.getDisplayName(), null);
        telegramNotificationService.sendPrivateText(rejected.getPrivateChatId(), telegramMessages.welcomeRejected(languageOf(rejected)), null);
    }

    private void handleAdminSummary(Long chatId, Long sessionId, String callbackQueryId) {
        SessionSummaryResponse summary = summaryService.buildSummary(sessionId);
        telegramNotificationService.answerCallback(callbackQueryId, "Summary ready");
        telegramNotificationService.sendPrivateText(chatId, summary.groupSummaryText(), null);
    }

    private void handleAdminClose(Long telegramUserId, Long sessionId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        OrderSession session = orderSessionService.closeSession(sessionId, actor.getId());
        telegramNotificationService.sendClosedSummary(summaryService.buildSummary(session.getId()), session.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "Session closed");
    }

    private void handleRestaurantVote(Long telegramUserId, Long voteSessionId, Long restaurantId, String callbackQueryId) {
        try {
            Optional<LunchUser> userOptional = userService.findByTelegramUserId(telegramUserId);
            if (userOptional.isEmpty()) {
                log.warn("restaurant_vote_unregistered_user telegramUserId={}", telegramUserId);
                telegramNotificationService.answerCallback(callbackQueryId, telegramMessages.voteRegistrationRequired());
                return;
            }

            LunchUser user = userOptional.get();
            if (user.getStatus() == UserStatus.PENDING) {
                log.warn("restaurant_vote_pending_user telegramUserId={} userId={}", telegramUserId, user.getId());
                telegramNotificationService.answerCallback(callbackQueryId, telegramMessages.votePendingApproval());
                return;
            }
            if (user.getStatus() == UserStatus.REJECTED || user.getStatus() == UserStatus.BLOCKED) {
                log.warn(
                        "restaurant_vote_blocked_user telegramUserId={} userId={} status={}",
                        telegramUserId,
                        user.getId(),
                        user.getStatus()
                );
                telegramNotificationService.answerCallback(callbackQueryId, telegramMessages.voteAccessDenied());
                return;
            }

            restaurantVoteSessionService.castVote(telegramUserId, voteSessionId, restaurantId);

            telegramNotificationService.answerCallback(
                    callbackQueryId,
                    languageOf(user) == UserLanguage.RU
                            ? "Ваш голос сохранён."
                            : "Ovozingiz saqlandi."
            );
        } catch (NotFoundException exception) {
            log.warn("restaurant_vote_not_found telegramUserId={} reason={}", telegramUserId, exception.getMessage());
            telegramNotificationService.answerCallback(callbackQueryId, telegramMessages.voteClosed());
        } catch (BadRequestException exception) {
            telegramNotificationService.answerCallback(
                    callbackQueryId,
                    telegramMessages.voteClosed().equals(exception.getMessage()) ? telegramMessages.voteClosed() : telegramMessages.voteFailed()
            );
        } catch (ForbiddenException exception) {
            telegramNotificationService.answerCallback(callbackQueryId, telegramMessages.voteAccessDenied());
        } catch (Exception exception) {
            log.error(
                    "restaurant_vote_failed telegramUserId={} voteSessionId={} restaurantId={} reason={}",
                    telegramUserId,
                    voteSessionId,
                    restaurantId,
                    exception.getMessage(),
                    exception
            );

            telegramNotificationService.answerCallback(
                    callbackQueryId,
                    telegramMessages.voteFailed()
            );
        }
    }

    private void handleRestaurantWinner(Long telegramUserId, Long voteSessionId, Long restaurantId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        restaurantVoteSessionService.chooseWinner(voteSessionId, restaurantId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "Winner selected");
    }

    private void handleDeclareCash(Long telegramUserId, Long paymentId, String callbackQueryId) {
        paymentService.declareCashPayment(telegramUserId, paymentId);
        telegramNotificationService.answerCallback(callbackQueryId, resolveLanguage(telegramUserId) == UserLanguage.RU ? "Выбран наличный платёж." : "Naqd to'lov tanlandi.");
    }

    private void handleUploadReceiptPrompt(Long chatId, Long telegramUserId, String callbackQueryId) {
        UserLanguage language = resolveLanguage(telegramUserId);
        telegramNotificationService.answerCallback(callbackQueryId, language == UserLanguage.RU ? "Отправьте фото чека." : "Chek rasmini yuboring.");
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.uploadReceiptPrompt(language), null);
    }

    private void handleApprovePayment(Long telegramUserId, Long paymentId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        paymentService.approvePayment(paymentId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "Payment approved");
    }

    private void handlePaymentStatus(Long telegramUserId, Long paymentId, String callbackQueryId) {
        telegramNotificationService.answerCallback(callbackQueryId, paymentService.getUserPaymentStatusText(telegramUserId, paymentId));
    }

    private void handleRejectPayment(Long telegramUserId, Long paymentId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        paymentService.rejectPayment(paymentId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "Payment rejected");
    }

    private void handleMarkCashPaid(Long telegramUserId, Long paymentId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        paymentService.markCashPaid(paymentId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "Cash payment marked as paid");
    }

    private UserLanguage resolveLanguage(Long telegramUserId) {
        return userService.findByTelegramUserId(telegramUserId)
                .map(this::languageOf)
                .orElseGet(() -> registrationStateService.getLanguage(telegramUserId).orElse(UserLanguage.UZ));
    }

    private UserLanguage languageOf(LunchUser user) {
        return user == null || user.getLanguage() == null ? UserLanguage.UZ : user.getLanguage();
    }

    private boolean isRegistrationComplete(LunchUser user) {
        return user != null && user.getPrivateChatId() != null && user.getPhoneNumber() != null && user.getLanguage() != null;
    }
}
