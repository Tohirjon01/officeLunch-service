package uz.company.lunchbot.bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.UserOrderService;
import uz.company.lunchbot.service.UserService;
import uz.company.lunchbot.service.notification.TelegramNotificationService;
import uz.company.lunchbot.service.report.SummaryService;
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
    private final SummaryService summaryService;
    private final TelegramNotificationService telegramNotificationService;
    private final TelegramMessages telegramMessages;

    public void handle(CallbackQuery callbackQuery) {
        ParsedCallback parsed = callbackDataParser.parse(callbackQuery.getData());
        Long chatId = callbackQuery.getMessage().getChatId();
        Long telegramUserId = callbackQuery.getFrom().getId();

        try {
            switch (parsed.action()) {
                case "USER_ORDER_MENU" -> handleUserOrder(chatId, telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "USER_ORDER_SKIP" -> handleUserSkip(chatId, telegramUserId, callbackQuery.getId());
                case "ADMIN_CONFIRM_SESSION" -> handleAdminConfirm(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_CANCEL_SESSION" -> handleAdminCancel(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_EXTEND_SESSION" -> handleAdminExtend(telegramUserId, Long.parseLong(parsed.args().get(0)), Integer.parseInt(parsed.args().get(1)), callbackQuery.getId());
                case "ADMIN_APPROVE_USER" -> handleAdminApproveUser(chatId, telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_REJECT_USER" -> handleAdminRejectUser(chatId, telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_SUMMARY" -> handleAdminSummary(chatId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                case "ADMIN_CLOSE_SESSION" -> handleAdminClose(telegramUserId, Long.parseLong(parsed.args().getFirst()), callbackQuery.getId());
                default -> telegramNotificationService.answerCallback(callbackQuery.getId(), "Unknown action");
            }
        } catch (Exception exception) {
            log.error("telegram_callback_failed data={} reason={}", callbackQuery.getData(), exception.getMessage(), exception);
            telegramNotificationService.answerCallback(callbackQuery.getId(), exception.getMessage());
        }
    }

    private void handleUserOrder(Long chatId, Long telegramUserId, Long menuItemId, String callbackQueryId) {
        String oldMeal = userOrderService.getTodayOrderForTelegramUser(telegramUserId)
                .filter(order -> order.getMenuItem() != null)
                .map(order -> order.getMenuItem().getName())
                .orElse(null);

        UserOrder order = userOrderService.placeTodayOrder(telegramUserId, menuItemId);
        telegramNotificationService.answerCallback(callbackQueryId, "Order saved");
        String message = oldMeal == null
                ? telegramMessages.orderAccepted(order.getMenuItem())
                : telegramMessages.orderUpdated(oldMeal, order.getMenuItem().getName());
        telegramNotificationService.sendPrivateText(chatId, message, null);
    }

    private void handleUserSkip(Long chatId, Long telegramUserId, String callbackQueryId) {
        userOrderService.skipToday(telegramUserId);
        telegramNotificationService.answerCallback(callbackQueryId, "Skipped for today");
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.skippedToday(), null);
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

    private void handleAdminApproveUser(Long chatId, Long telegramUserId, Long targetUserId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        LunchUser approved = userService.approve(targetUserId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "User approved");
        telegramNotificationService.sendPrivateText(chatId, "Approved: " + approved.getDisplayName(), null);
        telegramNotificationService.sendApprovedMainMenu(approved, "Your registration has been approved.");
    }

    private void handleAdminRejectUser(Long chatId, Long telegramUserId, Long targetUserId, String callbackQueryId) {
        LunchUser actor = userService.getApprovedUserByTelegramUserId(telegramUserId);
        LunchUser rejected = userService.reject(targetUserId, actor.getId());
        telegramNotificationService.answerCallback(callbackQueryId, "User rejected");
        telegramNotificationService.sendPrivateText(chatId, "Rejected: " + rejected.getDisplayName(), null);
        telegramNotificationService.sendPrivateText(rejected.getPrivateChatId(), telegramMessages.welcomeRejected(), null);
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
}
