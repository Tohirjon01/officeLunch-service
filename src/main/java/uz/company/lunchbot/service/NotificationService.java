package uz.company.lunchbot.service;

import java.util.List;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;

public interface NotificationService {

    void sendPrivateText(Long chatId, String text, Object keyboard);

    Long sendGroupText(String text, Object keyboard);

    void editGroupText(Long messageId, String text, Object keyboard);

    void editText(Long chatId, Long messageId, String text, Object keyboard);

    void copyPrivateMessage(Long chatId, Long sourceChatId, Long sourceMessageId);

    void answerCallback(String callbackQueryId, String text);

    void sendApprovedMainMenu(LunchUser user, String text);

    void sendTodayMenu(Long chatId, OrderSession session);

    void sendMenuSelection(Long chatId, OrderSession session);

    void sendMenuCategorySelection(Long chatId, OrderSession session, int categoryIndex, int page);

    void sendMenuItemPreview(Long chatId, OrderSession session, MenuItem menuItem);

    void notifyAdminsAboutPendingUser(LunchUser pendingUser);

    void sendGroupOpenAnnouncement(OrderSession session);

    void sendReminder(OrderSession session, List<LunchUser> notRespondedUsers);

    void sendClosedSummary(SessionSummaryResponse summary, Long sessionId);

    void sendConfirmedSummary(SessionSummaryResponse summary);

    void sendPendingUsersList(Long chatId, List<LunchUser> pendingUsers);
}
