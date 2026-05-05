package uz.company.lunchbot.bot.message;

import java.util.List;
import org.springframework.stereotype.Component;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.util.MoneyUtils;

@Component
public class TelegramMessages {

    public String welcomeNewPending() {
        return "Welcome. Your registration request has been sent to admin. Please wait for approval.";
    }

    public String welcomePending() {
        return "Your registration is pending admin approval.";
    }

    public String welcomeRejected() {
        return "Your registration was rejected. Please contact admin.";
    }

    public String approvedMenuGreeting(LunchUser user) {
        return "Welcome, " + user.getDisplayName() + ". Choose an action from the menu below.";
    }

    public String help() {
        return "Use the private chat to see today's menu, place or update your order, check your order, or skip today.";
    }

    public String noSessionToday() {
        return "There is no lunch session for today yet.";
    }

    public String sessionClosed() {
        return "❌ Today's order is already closed.";
    }

    public String todayMenu(OrderSession session, List<MenuItem> menuItems) {
        String menuLines = menuItems.stream()
                .map(item -> "- " + item.getName() + " — " + MoneyUtils.formatUzs(item.getPrice()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No active menu items.");

        return """
                🍽 Today's lunch menu

                Restaurant: %s
                Deadline: %s

                %s
                """.formatted(session.getRestaurant().getName(), session.getDeadlineAt().toLocalTime(), menuLines);
    }

    public String chooseMenuItem() {
        return "Choose your meal from today's menu.";
    }

    public String orderAccepted(MenuItem item) {
        return "✅ Your order has been accepted:\nMeal: %s\nPrice: %s\nYou can change it before the deadline."
                .formatted(item.getName(), MoneyUtils.formatUzs(item.getPrice()));
    }

    public String orderUpdated(String oldMeal, String newMeal) {
        return "♻️ Your order has been updated:\nOld: %s\nNew: %s".formatted(oldMeal, newMeal);
    }

    public String skippedToday() {
        return "✅ Marked as skipped for today.";
    }

    public String myOrder(UserOrder order) {
        if (order.getStatus() == UserOrderStatus.SKIPPED) {
            return "You are marked as skipped for today.";
        }
        if (order.getStatus() == UserOrderStatus.CANCELLED) {
            return "Your order was cancelled.";
        }
        return """
                Your order:
                Meal: %s
                Status: %s
                Final price: %s
                """.formatted(
                order.getMenuItem() == null ? "N/A" : order.getMenuItem().getName(),
                order.getStatus(),
                MoneyUtils.formatUzs(order.getFinalPrice()));
    }

    public String noResponseYet() {
        return "You have not responded yet.";
    }

    public String registrationRequired() {
        return "Please open a private chat with the bot and send /start first.";
    }

    public String pendingUserCard(LunchUser user) {
        return "Pending user:\n" + user.getDisplayName() + "\nTelegram ID: " + user.getTelegramUserId();
    }

    public String noPendingUsers() {
        return "There are no pending users right now.";
    }

    public String restaurantManagementHelp() {
        return """
                Restaurant management commands:
                /restaurants
                /restaurant_create Name | phone-or-- | address-or-- | default(true/false) | active(true/false) | containerEnabled(true/false) | defaultContainerPrice | deliveryEnabled(true/false) | defaultDeliveryPrice
                /restaurant_update id | Name | phone-or-- | address-or--
                /restaurant_activate id
                /restaurant_deactivate id
                /restaurant_default id
                /restaurant_container id | enabled(true/false) | defaultContainerPrice
                /restaurant_delivery id | enabled(true/false) | defaultDeliveryPrice
                """;
    }

    public String menuManagementHelp() {
        return """
                Menu management commands:
                /menu restaurantId
                /menu_add restaurantId | Name | price | active(true/false) | containerRequired(true/false/null) | containerPriceOverride-or--
                /menu_update id | Name | price
                /menu_activate id
                /menu_deactivate id
                /menu_container id | containerRequired(true/false/null) | containerPriceOverride-or--
                /menu_container_override_remove id
                """;
    }

    public String sessionPricingHelp() {
        return """
                Session pricing commands:
                /session_delivery amount
                /session_recalculate
                /session_recalculate allow_confirmed
                """;
    }

    public String featureHandledViaApi(String feature) {
        return feature + " is available through the internal REST API in this MVP.";
    }

    public String groupOpenAnnouncement(OrderSession session) {
        return """
                🍽 Today’s lunch order is open

                Restaurant: %s
                Deadline: %s

                Please place your order before the deadline.
                Press the button below to order.
                """.formatted(session.getRestaurant().getName(), session.getDeadlineAt().toLocalTime());
    }

    public String notRespondedReminder(OrderSession session, List<LunchUser> users) {
        String userLines = users.isEmpty()
                ? "Everyone has responded."
                : users.stream()
                .map(LunchUser::getDisplayName)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("Everyone has responded.");

        return """
                ⏰ Lunch order reminder

                The following users have not responded yet:
                %s

                Deadline: %s
                """.formatted(userLines, session.getDeadlineAt().toLocalTime());
    }

    public String privateReminder(OrderSession session) {
        return "Reminder: today's lunch session is still open until " + session.getDeadlineAt().toLocalTime() + ".";
    }

    public String sessionClosedShort(long orderedCount, long skippedCount, long noResponseCount) {
        return """
                🔒 Lunch order is closed

                Ordered: %d
                Skipped: %d
                No response: %d

                Admin confirmation is required.
                """.formatted(orderedCount, skippedCount, noResponseCount);
    }

    public String sessionConfirmed() {
        return "✅ Order session confirmed.";
    }

    public String sessionExtended(OrderSession session) {
        return "Session deadline extended to " + session.getDeadlineAt().toLocalTime() + ".";
    }

    public String sessionCancelled() {
        return "Session cancelled.";
    }

    public String actionCompleted(String action) {
        return action + " completed.";
    }

    public String accessDenied() {
        return "You are not allowed to perform this action.";
    }
}
