package uz.company.lunchbot.bot.keyboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import uz.company.lunchbot.bot.message.TelegramMenuLabels;
import uz.company.lunchbot.entity.MenuItem;

@Component
public class TelegramKeyboards {

    public Map<String, Object> mainMenu(boolean admin) {
        List<List<Map<String, String>>> keyboard = admin
                ? List.of(
                buttonRow(TelegramMenuLabels.TODAYS_MENU, TelegramMenuLabels.PLACE_ORDER),
                buttonRow(TelegramMenuLabels.MY_ORDER, TelegramMenuLabels.SKIP_TODAY),
                buttonRow(TelegramMenuLabels.TODAYS_SUMMARY, TelegramMenuLabels.NOT_RESPONDED_USERS),
                buttonRow(TelegramMenuLabels.CLOSE_ORDER, TelegramMenuLabels.CONFIRM_ORDER),
                buttonRow(TelegramMenuLabels.EXTEND_DEADLINE, TelegramMenuLabels.PENDING_USERS),
                buttonRow(TelegramMenuLabels.MANAGE_MENU, TelegramMenuLabels.MANUAL_ORDER_EDIT),
                buttonRow(TelegramMenuLabels.SET_DELIVERY_PRICE, TelegramMenuLabels.SET_CONTAINER_PRICE),
                buttonRow(TelegramMenuLabels.HELP))
                : List.of(
                buttonRow(TelegramMenuLabels.TODAYS_MENU, TelegramMenuLabels.PLACE_ORDER),
                buttonRow(TelegramMenuLabels.MY_ORDER, TelegramMenuLabels.SKIP_TODAY),
                buttonRow(TelegramMenuLabels.HELP));

        return Map.of(
                "keyboard", keyboard,
                "resize_keyboard", true,
                "one_time_keyboard", false);
    }

    public Map<String, Object> menuSelection(List<MenuItem> menuItems) {
        List<List<Map<String, String>>> inlineKeyboard = menuItems.stream()
                .map(item -> List.of(callbackButton(item.getName(), "USER_ORDER_MENU:" + item.getId())))
                .toList();

        inlineKeyboard = new java.util.ArrayList<>(inlineKeyboard);
        inlineKeyboard.add(List.of(callbackButton("Skip Today", "USER_ORDER_SKIP")));

        return Map.of("inline_keyboard", inlineKeyboard);
    }

    public Map<String, Object> groupPlaceOrderButton(String botUsername) {
        return Map.of("inline_keyboard", List.of(List.of(urlButton("Place Order", "https://t.me/" + botUsername))));
    }

    public Map<String, Object> adminSessionActions(Long sessionId) {
        return Map.of("inline_keyboard", List.of(
                List.of(callbackButton("Confirm Order", "ADMIN_CONFIRM_SESSION:" + sessionId)),
                List.of(callbackButton("Extend 10 Minutes", "ADMIN_EXTEND_SESSION:" + sessionId + ":10")),
                List.of(callbackButton("Show Summary", "ADMIN_SUMMARY:" + sessionId)),
                List.of(callbackButton("Cancel Session", "ADMIN_CANCEL_SESSION:" + sessionId))));
    }

    public Map<String, Object> pendingUserActions(Long userId) {
        return Map.of("inline_keyboard", List.of(
                List.of(
                        callbackButton("Approve", "ADMIN_APPROVE_USER:" + userId),
                        callbackButton("Reject", "ADMIN_REJECT_USER:" + userId))));
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
