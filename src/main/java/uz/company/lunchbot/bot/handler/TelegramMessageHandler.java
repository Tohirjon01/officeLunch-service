package uz.company.lunchbot.bot.handler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import uz.company.lunchbot.bot.message.TelegramMenuLabels;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.dto.request.RecalculateSessionRequest;
import uz.company.lunchbot.dto.request.TelegramRegistrationRequest;
import uz.company.lunchbot.dto.response.RegistrationResultResponse;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.RecalculationMode;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.TelegramAdminCommandService;
import uz.company.lunchbot.service.UserOrderService;
import uz.company.lunchbot.service.UserService;
import uz.company.lunchbot.service.notification.TelegramNotificationService;
import uz.company.lunchbot.service.report.SummaryService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMessageHandler {

    private final UserService userService;
    private final OrderSessionService orderSessionService;
    private final UserOrderService userOrderService;
    private final SummaryService summaryService;
    private final TelegramNotificationService telegramNotificationService;
    private final TelegramMessages telegramMessages;
    private final TelegramAdminCommandService telegramAdminCommandService;

    public void handle(Message message) {
        if (!message.hasText() || !isPrivateChat(message)) {
            return;
        }

        String text = message.getText().trim();
        Long chatId = message.getChatId();
        Long telegramUserId = message.getFrom().getId();

        try {
            if (text.startsWith("/start")) {
                handleStart(message);
                return;
            }

            LunchUser user = userService.getRequiredByTelegramUserId(telegramUserId);
            if (user.getStatus() != UserStatus.APPROVED) {
                sendStatusAwareMessage(user, chatId);
                return;
            }

            switch (text) {
                case TelegramMenuLabels.TODAYS_MENU -> handleTodaysMenu(chatId);
                case TelegramMenuLabels.PLACE_ORDER -> handlePlaceOrder(chatId);
                case TelegramMenuLabels.MY_ORDER -> handleMyOrder(chatId, telegramUserId);
                case TelegramMenuLabels.SKIP_TODAY -> handleSkipToday(telegramUserId, user);
                case TelegramMenuLabels.HELP -> telegramNotificationService.sendPrivateText(chatId, telegramMessages.help(), null);
                default -> handleAdminOrFallback(text, user, chatId);
            }
        } catch (NotFoundException exception) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.registrationRequired(), null);
        } catch (Exception exception) {
            log.error("telegram_message_failed chatId={} reason={}", chatId, exception.getMessage(), exception);
            telegramNotificationService.sendPrivateText(chatId, exception.getMessage(), null);
        }
    }

    private void handleStart(Message message) {
        RegistrationResultResponse result = userService.registerTelegramUser(new TelegramRegistrationRequest(
                message.getFrom().getId(),
                message.getFrom().getUserName(),
                message.getFrom().getFirstName(),
                message.getFrom().getLastName(),
                message.getChatId()));

        LunchUser user = result.user();
        if (result.created()) {
            telegramNotificationService.notifyAdminsAboutPendingUser(user);
            telegramNotificationService.sendPrivateText(message.getChatId(), telegramMessages.welcomeNewPending(), null);
            return;
        }
        sendStatusAwareMessage(user, message.getChatId());
    }

    private void sendStatusAwareMessage(LunchUser user, Long chatId) {
        if (user.getStatus() == UserStatus.PENDING) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.welcomePending(), null);
        } else if (user.getStatus() == UserStatus.REJECTED) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.welcomeRejected(), null);
        } else if (user.getStatus() == UserStatus.BLOCKED) {
            telegramNotificationService.sendPrivateText(chatId, telegramMessages.accessDenied(), null);
        } else {
            telegramNotificationService.sendApprovedMainMenu(user, telegramMessages.approvedMenuGreeting(user));
        }
    }

    private void handleTodaysMenu(Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday()));
        telegramNotificationService.sendTodayMenu(chatId, session);
    }

    private void handlePlaceOrder(Long chatId) {
        OrderSession session = orderSessionService.getActiveOrderingSession();
        telegramNotificationService.sendMenuSelection(chatId, session.getRestaurant().getId());
    }

    private void handleMyOrder(Long chatId, Long telegramUserId) {
        UserOrder order = userOrderService.getTodayOrderForTelegramUser(telegramUserId)
                .orElse(null);
        telegramNotificationService.sendPrivateText(chatId, order == null ? telegramMessages.noResponseYet() : telegramMessages.myOrder(order), null);
    }

    private void handleSkipToday(Long telegramUserId, LunchUser user) {
        userOrderService.skipToday(telegramUserId);
        telegramNotificationService.sendApprovedMainMenu(user, telegramMessages.skippedToday());
    }

    private void handleAdminOrFallback(String text, LunchUser user, Long chatId) {
        boolean admin = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN;
        if (!admin) {
            telegramNotificationService.sendApprovedMainMenu(user, telegramMessages.help());
            return;
        }

        if (telegramAdminCommandService.supports(text)) {
            telegramNotificationService.sendPrivateText(chatId, telegramAdminCommandService.handle(user, text), null);
            return;
        }

        switch (text) {
            case TelegramMenuLabels.TODAYS_SUMMARY -> sendTodaySummary(chatId);
            case TelegramMenuLabels.NOT_RESPONDED_USERS -> sendNotRespondedUsers(chatId);
            case TelegramMenuLabels.PENDING_USERS -> telegramNotificationService.sendPendingUsersList(chatId, userService.getPendingUsers());
            case TelegramMenuLabels.CLOSE_ORDER -> closeTodayOrder(user, chatId);
            case TelegramMenuLabels.CONFIRM_ORDER -> confirmTodayOrder(user, chatId);
            case TelegramMenuLabels.EXTEND_DEADLINE -> extendTodayOrder(user, chatId);
            case TelegramMenuLabels.MANAGE_RESTAURANTS -> telegramNotificationService.sendPrivateText(chatId, telegramMessages.restaurantManagementHelp(), null);
            case TelegramMenuLabels.MANAGE_MENU -> telegramNotificationService.sendPrivateText(chatId, telegramMessages.menuManagementHelp(), null);
            case TelegramMenuLabels.SET_CURRENT_SESSION_DELIVERY_PRICE -> telegramNotificationService.sendPrivateText(chatId, telegramMessages.sessionPricingHelp(), null);
            case TelegramMenuLabels.RECALCULATE_CURRENT_SESSION -> recalculateCurrentSession(user, chatId);
            case TelegramMenuLabels.MANUAL_ORDER_EDIT -> telegramNotificationService.sendPrivateText(chatId, telegramMessages.featureHandledViaApi("Manual Order Edit"), null);
            default -> telegramNotificationService.sendApprovedMainMenu(user, telegramMessages.help());
        }
    }

    private void sendTodaySummary(Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday()));
        SessionSummaryResponse summary = summaryService.buildSummary(session.getId());
        telegramNotificationService.sendPrivateText(chatId, summary.groupSummaryText(), null);
    }

    private void sendNotRespondedUsers(Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday()));
        List<LunchUser> users = userOrderService.findNotRespondedUsers(session);
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.notRespondedReminder(session, users), null);
    }

    private void closeTodayOrder(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodayOpenSession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday()));
        OrderSession closed = orderSessionService.closeSession(session.getId(), actor.getId());
        SessionSummaryResponse summary = summaryService.buildSummary(closed.getId());
        telegramNotificationService.sendClosedSummary(summary, closed.getId());
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.actionCompleted("Close order"), null);
    }

    private void confirmTodayOrder(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday()));
        OrderSession confirmed = orderSessionService.confirmSession(session.getId(), actor.getId());
        telegramNotificationService.sendConfirmedSummary(summaryService.buildSummary(confirmed.getId()));
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.actionCompleted("Confirm order"), null);
    }

    private void extendTodayOrder(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday()));
        OrderSession extended = orderSessionService.extendSession(session.getId(), 10, actor.getId());
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.sessionExtended(extended), null);
    }

    private void recalculateCurrentSession(LunchUser actor, Long chatId) {
        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException(telegramMessages.noSessionToday()));
        RecalculateSessionRequest request = new RecalculateSessionRequest(RecalculationMode.FULL_PRICE_REBUILD, false);
        orderSessionService.recalculateSession(session.getId(), request, actor.getId());
        telegramNotificationService.sendPrivateText(chatId, telegramMessages.actionCompleted("Current session recalculated"), null);
    }

    private boolean isPrivateChat(Message message) {
        return message.getChat() != null && "private".equalsIgnoreCase(message.getChat().getType());
    }
}
