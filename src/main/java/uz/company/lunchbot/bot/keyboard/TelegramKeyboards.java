package uz.company.lunchbot.bot.keyboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import uz.company.lunchbot.bot.message.TelegramMenuLabels;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.enums.UserLanguage;

@Component
public class TelegramKeyboards {

    public Map<String, Object> mainMenu(UserLanguage language, boolean admin) {
        List<List<Map<String, String>>> keyboard = admin
                ? List.of(
                buttonRow(TelegramMenuLabels.todaysMenu(language), TelegramMenuLabels.placeOrder(language)),
                buttonRow(TelegramMenuLabels.myOrder(language), TelegramMenuLabels.skipToday(language)),
                buttonRow(TelegramMenuLabels.todaysSummary(language), TelegramMenuLabels.notRespondedUsers(language)),
                buttonRow(TelegramMenuLabels.closeOrder(language), TelegramMenuLabels.confirmOrder(language)),
                buttonRow(TelegramMenuLabels.extendDeadline(language), TelegramMenuLabels.pendingUsers(language)),
                buttonRow(TelegramMenuLabels.paymentSummary(language), TelegramMenuLabels.help(language)),
                buttonRow(TelegramMenuLabels.manageRestaurants(language), TelegramMenuLabels.manageMenu(language)),
                buttonRow(TelegramMenuLabels.setCurrentSessionDeliveryPrice(language), TelegramMenuLabels.recalculateCurrentSession(language)),
                buttonRow(TelegramMenuLabels.manualOrderEdit(language), TelegramMenuLabels.changeLanguage(language)))
                : List.of(
                buttonRow(TelegramMenuLabels.todaysMenu(language), TelegramMenuLabels.placeOrder(language)),
                buttonRow(TelegramMenuLabels.myOrder(language), TelegramMenuLabels.skipToday(language)),
                buttonRow(TelegramMenuLabels.help(language), TelegramMenuLabels.changeLanguage(language)));

        return Map.of(
                "keyboard", keyboard,
                "resize_keyboard", true,
                "one_time_keyboard", false);
    }

    public Map<String, Object> menuSelection(OrderSession session, List<String> categories, UserLanguage language) {
        List<List<Map<String, String>>> inlineKeyboard = new java.util.ArrayList<>(categories.size() + 1);
        for (int i = 0; i < categories.size(); i++) {
            inlineKeyboard.add(List.of(callbackButton(categories.get(i), "MENU_CATEGORY:" + session.getId() + ":" + i + ":0")));
        }

        inlineKeyboard.add(List.of(callbackButton(language == UserLanguage.RU ? "Сегодня не беру" : "Bugun olmayman", "USER_ORDER_SKIP")));

        return Map.of("inline_keyboard", inlineKeyboard);
    }

    public Map<String, Object> menuCategoryItems(
            OrderSession session,
            int categoryIndex,
            int page,
            int totalPages,
            List<MenuItem> menuItems,
            UserLanguage language
    ) {
        List<List<Map<String, String>>> inlineKeyboard = menuItems.stream()
                .map(item -> List.of(callbackButton(item.getName(), "MENU_ITEM_PREVIEW:" + session.getId() + ":" + item.getId())))
                .toList();

        inlineKeyboard = new java.util.ArrayList<>(inlineKeyboard);

        List<Map<String, String>> navigationButtons = new java.util.ArrayList<>();
        if (page > 0) {
            navigationButtons.add(callbackButton("Previous", "MENU_CATEGORY:" + session.getId() + ":" + categoryIndex + ":" + (page - 1)));
        }
        if (page < totalPages - 1) {
            navigationButtons.add(callbackButton("Next", "MENU_CATEGORY:" + session.getId() + ":" + categoryIndex + ":" + (page + 1)));
        }
        if (!navigationButtons.isEmpty()) {
            inlineKeyboard.add(navigationButtons);
        }

        inlineKeyboard.add(List.of(callbackButton(language == UserLanguage.RU ? "Назад к категориям" : "Kategoriyalarga qaytish", "MENU_SELECTION:" + session.getId())));
        inlineKeyboard.add(List.of(callbackButton(language == UserLanguage.RU ? "Сегодня не беру" : "Bugun olmayman", "USER_ORDER_SKIP")));

        return Map.of("inline_keyboard", inlineKeyboard);
    }

    public Map<String, Object> menuItemPreview(Long sessionId, Long menuItemId, UserLanguage language) {
        return Map.of("inline_keyboard", List.of(
                List.of(callbackButton(language == UserLanguage.RU ? "Выбрать" : "Tanlash", "ORDER_ITEM:" + sessionId + ":" + menuItemId)),
                List.of(callbackButton(language == UserLanguage.RU ? "Назад" : "Orqaga", "MENU_SELECTION:" + sessionId))
        ));
    }

    public Map<String, Object> menuPagination(Long restaurantId, int page, int totalPages) {
        if (totalPages <= 1) {
            return null;
        }

        List<Map<String, String>> buttons = new java.util.ArrayList<>();
        if (page > 0) {
            buttons.add(callbackButton("Previous", "MENU_PAGE:" + restaurantId + ":" + (page - 1)));
        }
        if (page < totalPages - 1) {
            buttons.add(callbackButton("Next", "MENU_PAGE:" + restaurantId + ":" + (page + 1)));
        }

        if (buttons.isEmpty()) {
            return null;
        }

        return Map.of("inline_keyboard", List.of(buttons));
    }

    public Map<String, Object> languageSelection() {
        return Map.of("inline_keyboard", List.of(
                List.of(callbackButton("🇺🇿 O'zbek", "LANG:UZ")),
                List.of(callbackButton("🇷🇺 Русский", "LANG:RU"))
        ));
    }

    public Map<String, Object> contactRequest(UserLanguage language) {
        return Map.of(
                "keyboard", List.of(List.of(Map.of(
                        "text", language == UserLanguage.RU ? "Отправить номер телефона" : "Telefon raqamni yuborish",
                        "request_contact", true
                ))),
                "resize_keyboard", true,
                "one_time_keyboard", true
        );
    }

    public Map<String, Object> removeKeyboard() {
        return Map.of("remove_keyboard", true);
    }

    public Map<String, Object> groupPlaceOrderButton(String botUsername) {
        return Map.of("inline_keyboard", List.of(List.of(urlButton("Order Now", "https://t.me/" + botUsername + "?start=order"))));
    }

    public Map<String, Object> restaurantVoting(List<Restaurant> restaurants, Long voteSessionId) {
        List<List<Map<String, String>>> inlineKeyboard = restaurants.stream()
                .map(restaurant -> List.of(callbackButton(restaurant.getName(), "RESTAURANT_VOTE:" + voteSessionId + ":" + restaurant.getId())))
                .toList();
        return Map.of("inline_keyboard", inlineKeyboard);
    }

    public Map<String, Object> restaurantWinnerSelection(Long voteSessionId, List<Restaurant> restaurants) {
        List<List<Map<String, String>>> inlineKeyboard = restaurants.stream()
                .map(restaurant -> List.of(callbackButton(restaurant.getName(), "RESTAURANT_WINNER:" + voteSessionId + ":" + restaurant.getId())))
                .toList();
        return Map.of("inline_keyboard", inlineKeyboard);
    }

    public Map<String, Object> cashPaymentButton(Long paymentId) {
        return Map.of("inline_keyboard", List.of(
                List.of(callbackButton("Upload Receipt", "PAYMENT_UPLOAD_RECEIPT:" + paymentId)),
                List.of(callbackButton("Naqd to'layman", "PAYMENT_CASH:" + paymentId)),
                List.of(callbackButton("To'lov holati", "PAYMENT_STATUS:" + paymentId))
        ));
    }

    public Map<String, Object> cardPaymentAdminActions(Long paymentId) {
        return Map.of("inline_keyboard", List.of(List.of(
                callbackButton("✅ Approve", "admin_payment_approve_" + paymentId),
                callbackButton("❌ Reject", "admin_payment_reject_" + paymentId)
        )));
    }

    public Map<String, Object> cashPaymentAdminActions(Long paymentId) {
        return Map.of("inline_keyboard", List.of(List.of(
                callbackButton("Cash Received", "PAYMENT_MARK_CASH_PAID:" + paymentId),
                callbackButton("Reject", "PAYMENT_REJECT:" + paymentId)
        )));
    }

    public Map<String, Object> adminSessionActions(Long sessionId) {
        return Map.of("inline_keyboard", List.of(
                List.of(callbackButton("Confirm Order", "ADMIN_CONFIRM_SESSION:" + sessionId)),
                List.of(callbackButton("Extend 10 Minutes", "ADMIN_EXTEND_SESSION:" + sessionId + ":10")),
                List.of(callbackButton("Show Summary", "ADMIN_SUMMARY:" + sessionId)),
                List.of(callbackButton("Cancel Session", "ADMIN_CANCEL_SESSION:" + sessionId))));
    }

    public Map<String, Object> orderControl(Long sessionId) {
        return Map.of("inline_keyboard", List.of(
                List.of(
                        callbackButton("➕ 10 min", "ORDER_EXTEND:" + sessionId + ":10"),
                        callbackButton("➖ 10 min", "ORDER_REDUCE:" + sessionId + ":10")
                ),
                List.of(
                        callbackButton("➕ 30 min", "ORDER_EXTEND:" + sessionId + ":30"),
                        callbackButton("🔒 Hozir yopish", "ORDER_CLOSE_NOW:" + sessionId)
                ),
                List.of(
                        callbackButton("📊 Summary", "ORDER_SUMMARY:" + sessionId),
                        callbackButton("🔄 Refresh", "ORDER_CONTROL_REFRESH:" + sessionId)
                )
        ));
    }

    public Map<String, Object> voteControl(Long voteSessionId) {
        return Map.of("inline_keyboard", List.of(
                List.of(
                        callbackButton("➕ 5 min", "VOTE_EXTEND:" + voteSessionId + ":5"),
                        callbackButton("➖ 5 min", "VOTE_REDUCE:" + voteSessionId + ":5")
                ),
                List.of(callbackButton("🔒 Votingni yopish", "VOTE_CLOSE_NOW:" + voteSessionId)),
                List.of(
                        callbackButton("📊 Natija", "VOTE_RESULT:" + voteSessionId),
                        callbackButton("🔄 Refresh", "VOTE_CONTROL_REFRESH:" + voteSessionId)
                )
        ));
    }

    public Map<String, Object> pendingUserActions(Long userId) {
        return Map.of("inline_keyboard", List.of(
                List.of(
                        callbackButton("Approve", "APPROVE_USER:" + userId),
                        callbackButton("Reject", "REJECT_USER:" + userId))));
    }

    private List<Map<String, String>> buttonRow(String... labels) {
        return java.util.Arrays.stream(labels)
                .map(label -> Map.of("text", label))
                .toList();
    }

    private Map<String, String> callbackButton(String text, String callbackData) {
        Map<String, String> button = new LinkedHashMap<>();
        button.put("text", text);
        button.put("callback_data", callbackData);
        return button;
    }

    private Map<String, String> urlButton(String text, String url) {
        Map<String, String> button = new LinkedHashMap<>();
        button.put("text", text);
        button.put("url", url);
        return button;
    }
}
