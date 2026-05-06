package uz.company.lunchbot.bot.message;

import uz.company.lunchbot.enums.UserLanguage;

public final class TelegramMenuLabels {

    private TelegramMenuLabels() {
    }

    public static String todaysMenu(UserLanguage language) {
        return isRu(language) ? "Сегодняшнее меню" : "Bugungi menyu";
    }

    public static String placeOrder(UserLanguage language) {
        return isRu(language) ? "Сделать заказ" : "Buyurtma berish";
    }

    public static String myOrder(UserLanguage language) {
        return isRu(language) ? "Мой заказ" : "Mening buyurtmam";
    }

    public static String skipToday(UserLanguage language) {
        return isRu(language) ? "Сегодня не беру" : "Bugun olmayman";
    }

    public static String help(UserLanguage language) {
        return isRu(language) ? "Помощь" : "Yordam";
    }

    public static String changeLanguage(UserLanguage language) {
        return isRu(language) ? "Изменить язык" : "Tilni o'zgartirish";
    }

    public static String todaysSummary(UserLanguage language) {
        return isRu(language) ? "Сегодняшний summary" : "Bugungi summary";
    }

    public static String closeOrder(UserLanguage language) {
        return isRu(language) ? "Закрыть заказ" : "Zakazni yopish";
    }

    public static String confirmOrder(UserLanguage language) {
        return isRu(language) ? "Подтвердить заказ" : "Zakazni tasdiqlash";
    }

    public static String extendDeadline(UserLanguage language) {
        return isRu(language) ? "Продлить дедлайн" : "Deadline uzaytirish";
    }

    public static String notRespondedUsers(UserLanguage language) {
        return isRu(language) ? "Не ответили" : "Javob bermaganlar";
    }

    public static String pendingUsers(UserLanguage language) {
        return isRu(language) ? "Pending users" : "Pending userlar";
    }

    public static String manageRestaurants(UserLanguage language) {
        return isRu(language) ? "Рестораны" : "Restoranlar";
    }

    public static String manageMenu(UserLanguage language) {
        return isRu(language) ? "Меню" : "Menyular";
    }

    public static String paymentSummary(UserLanguage language) {
        return isRu(language) ? "Сводка оплат" : "Payment summary";
    }

    public static String setCurrentSessionDeliveryPrice(UserLanguage language) {
        return isRu(language) ? "Доставка сессии" : "Session delivery";
    }

    public static String recalculateCurrentSession(UserLanguage language) {
        return isRu(language) ? "Пересчитать сессию" : "Session recalculation";
    }

    public static String manualOrderEdit(UserLanguage language) {
        return isRu(language) ? "Редактировать заказ" : "Manual order edit";
    }

    private static boolean isRu(UserLanguage language) {
        return language == UserLanguage.RU;
    }
}
