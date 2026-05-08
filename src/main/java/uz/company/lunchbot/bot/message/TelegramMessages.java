package uz.company.lunchbot.bot.message;

import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.PaymentRecordStatus;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserOrderStatus;
import java.math.BigDecimal;
import uz.company.lunchbot.util.MoneyUtils;

@Component
public class TelegramMessages {

    public String chooseLanguage() {
        return "Tilni tanlang / Выберите язык";
    }

    public String requestPhone(UserLanguage language) {
        return isRu(language)
                ? "Пожалуйста, отправьте ваш номер телефона через кнопку ниже."
                : "Iltimos, telefon raqamingizni quyidagi tugma orqali yuboring.";
    }

    public String registrationSubmitted(UserLanguage language) {
        return isRu(language)
                ? "Ваша заявка отправлена администратору. Ожидайте подтверждения."
                : "So'rovingiz adminlarga yuborildi. Tasdiqlanishini kuting.";
    }

    public String invalidContact(UserLanguage language) {
        return isRu(language)
                ? "Пожалуйста, отправьте именно свой контакт через кнопку Telegram."
                : "Iltimos, aynan o'zingizning kontaktingizni Telegram tugmasi orqali yuboring.";
    }

    public String languageUpdated(UserLanguage language) {
        return isRu(language) ? "Язык обновлен." : "Til yangilandi.";
    }

    public String welcomeNewPending() {
        return registrationSubmitted(UserLanguage.UZ);
    }

    public String welcomePending() {
        return welcomePending(UserLanguage.UZ);
    }

    public String welcomePending(UserLanguage language) {
        return isRu(language)
                ? "Ваша регистрация ожидает подтверждения администратора."
                : "Ro'yxatdan o'tishingiz admin tasdig'ini kutmoqda.";
    }

    public String welcomeRejected() {
        return welcomeRejected(UserLanguage.UZ);
    }

    public String welcomeRejected(UserLanguage language) {
        return isRu(language)
                ? "Доступ был отклонён. Обратитесь к администратору."
                : "Kirish rad etildi. Iltimos, admin bilan bog'laning.";
    }

    public String approvedMenuGreeting(LunchUser user) {
        UserLanguage language = user.getLanguage() == null ? UserLanguage.UZ : user.getLanguage();
        return isRu(language)
                ? "Добро пожаловать, " + user.getDisplayName() + ". Выберите действие из меню ниже."
                : "Xush kelibsiz, " + user.getDisplayName() + ". Quyidagi menyudan amalni tanlang.";
    }

    public String approved(UserLanguage language) {
        return isRu(language)
                ? "Ваша регистрация одобрена."
                : "Ro'yxatdan o'tishingiz tasdiqlandi.";
    }

    public String help() {
        return help(UserLanguage.UZ);
    }

    public String help(UserLanguage language) {
        return isRu(language)
                ? "Используйте личный чат, чтобы посмотреть меню на сегодня, оформить или изменить заказ, посмотреть свой заказ или отказаться на сегодня."
                : "Bugungi menyuni ko'rish, buyurtma berish yoki o'zgartirish, buyurtmangizni ko'rish yoki bugun olmayman deb belgilash uchun private chatdan foydalaning.";
    }

    public String noSessionToday() {
        return noSessionToday(UserLanguage.UZ);
    }

    public String noSessionToday(UserLanguage language) {
        return isRu(language) ? "На сегодня нет сессии заказа." : "Bugun order session yo'q.";
    }

    public String restaurantVotingStillOpen() {
        return restaurantVotingStillOpen(UserLanguage.UZ);
    }

    public String restaurantVotingStillOpen(UserLanguage language) {
        return isRu(language) ? "Голосование за ресторан ещё открыто." : "Restoran ovoz berishi hali ochiq.";
    }

    public String restaurantWinnerNotSelected() {
        return restaurantWinnerNotSelected(UserLanguage.UZ);
    }

    public String restaurantWinnerNotSelected(UserLanguage language) {
        return isRu(language) ? "Победитель ресторана ещё не выбран." : "Restoran g'olibi hali tanlanmagan.";
    }

    public String sessionClosed() {
        return sessionClosed(UserLanguage.UZ);
    }

    public String sessionClosed(UserLanguage language) {
        return isRu(language) ? "❌ Сегодняшний заказ уже закрыт." : "❌ Bugungi buyurtma allaqachon yopilgan.";
    }

    public String todayMenu(OrderSession session, List<MenuItem> menuItems) {
        return todayMenu(session, menuItems, UserLanguage.UZ);
    }

    public String todayMenu(OrderSession session, List<MenuItem> menuItems, UserLanguage language) {
        String menuLines = groupMenuByCategory(menuItems, language)
                .orElse("No active menu items.");

        return isRu(language)
                ? """
                Сегодняшнее меню — %s

                %s

                Чтобы заказать, нажмите "Сделать заказ".
                """.formatted(session.getRestaurant().getName(), menuLines)
                : """
                Bugungi menyu — %s

                %s

                Buyurtma berish uchun "Buyurtma berish" tugmasini bosing.
                """.formatted(session.getRestaurant().getName(), menuLines);
    }

    public String chooseMenuItem() {
        return chooseMenuItem(UserLanguage.UZ);
    }

    public String chooseMenuItem(UserLanguage language) {
        return isRu(language) ? "Выберите блюдо из сегодняшнего меню." : "Bugungi menyudan taom tanlang.";
    }

    public String voteRegistrationRequired() {
        return "Avval botga /start bosing va ro'yxatdan o'ting.";
    }

    public String votePendingApproval() {
        return "Ro'yxatdan o'tish so'rovingiz admin tasdig'ini kutmoqda.";
    }

    public String voteAccessDenied() {
        return "Sizga botdan foydalanish ruxsati berilmagan.";
    }

    public String voteClosed() {
        return "Ovoz berish yopilgan.";
    }

    public String voteFailed() {
        return "Ovoz saqlanmadi. Iltimos, keyinroq qayta urinib ko'ring.";
    }

    public String orderBlockedVotingOpen(UserLanguage language) {
        return isRu(language)
                ? "⏳ Сейчас идет голосование за ресторан. Прием заказов откроется после завершения голосования."
                : "⏳ Hozir restoran tanlash jarayoni davom etmoqda. Buyurtma voting tugagandan keyin ochiladi.";
    }

    public String orderBlockedWinnerNotSelected(UserLanguage language) {
        return isRu(language)
                ? "⚠️ Ресторан пока не выбран. Прием заказов откроется после выбора ресторана администратором."
                : "⚠️ Restoran hali tanlanmagan. Admin restoranni tanlagandan keyin buyurtma ochiladi.";
    }

    public String orderBlockedNoSession(UserLanguage language) {
        return isRu(language)
                ? "⏳ Заказ пока не открыт. Прием заказов откроется после выбора ресторана."
                : "⏳ Hozircha buyurtma ochilmagan. Buyurtma restoran tanlangandan keyin ochiladi.";
    }

    public String orderBlockedClosed(UserLanguage language) {
        return isRu(language)
                ? "🔒 Сегодняшний заказ закрыт. Следующий заказ откроется после нового голосования."
                : "🔒 Bugungi buyurtma yopilgan. Keyingi buyurtma yangi votingdan keyin ochiladi.";
    }

    public String orderBlockedDeadlinePassed(UserLanguage language) {
        return isRu(language)
                ? "🔒 Время приема заказов истекло."
                : "🔒 Buyurtma berish vaqti tugagan.";
    }

    public String chooseMenuCategory(OrderSession session, List<String> categoryLabels, UserLanguage language) {
        String categoryLines = IntStream.range(0, categoryLabels.size())
                .mapToObj(index -> (index + 1) + ". " + categoryLabels.get(index))
                .reduce((left, right) -> left + "\n" + right)
                .orElse(isRu(language) ? "Нет доступных категорий." : "Mavjud kategoriya yo'q.");

        return isRu(language)
                ? """
                %s

                Выберите категорию:
                %s
                """.formatted(session.getRestaurant().getName(), categoryLines)
                : """
                %s

                Kategoriyani tanlang:
                %s
                """.formatted(session.getRestaurant().getName(), categoryLines);
    }

    public String chooseMenuItemFromCategory(
            OrderSession session,
            String categoryLabel,
            List<MenuItem> menuItems,
            int page,
            int totalPages,
            UserLanguage language
    ) {
        String itemLines = IntStream.range(0, menuItems.size())
                .mapToObj(index -> (index + 1) + ". " + menuItems.get(index).getName() + " — " + MoneyUtils.formatUzs(menuItems.get(index).getPrice()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse(isRu(language) ? "Нет доступных блюд." : "Mavjud taom yo'q.");

        return isRu(language)
                ? """
                %s — %s — page %d/%d

                %s
                """.formatted(session.getRestaurant().getName(), categoryLabel, page + 1, totalPages, itemLines)
                : """
                %s — %s — page %d/%d

                %s
                """.formatted(session.getRestaurant().getName(), categoryLabel, page + 1, totalPages, itemLines);
    }

    public String menuItemPreview(MenuItem item, BigDecimal containerPrice, UserLanguage language) {
        BigDecimal normalizedContainerPrice = containerPrice == null ? BigDecimal.ZERO : containerPrice;
        BigDecimal approximateTotal = item.getPrice().add(normalizedContainerPrice);
        String containerLine = normalizedContainerPrice.signum() == 0
                ? (isRu(language) ? "Контейнер: не требуется" : "Idish: talab qilinmaydi")
                : (isRu(language) ? "Контейнер: " : "Idish: ") + MoneyUtils.formatUzs(normalizedContainerPrice);

        return isRu(language)
                ? """
                %s
                Цена: %s
                %s
                Примерно без доставки: %s
                """.formatted(item.getName(), MoneyUtils.formatUzs(item.getPrice()), containerLine, MoneyUtils.formatUzs(approximateTotal))
                : """
                %s
                Narxi: %s
                %s
                Taxminiy jami (dostavkasiz): %s
                """.formatted(item.getName(), MoneyUtils.formatUzs(item.getPrice()), containerLine, MoneyUtils.formatUzs(approximateTotal));
    }

    public String orderAccepted(MenuItem item) {
        return orderAccepted(item, UserLanguage.UZ);
    }

    public String orderAccepted(MenuItem item, UserLanguage language) {
        return isRu(language)
                ? "✅ Ваш заказ принят:\nБлюдо: %s\nЦена: %s\nВы можете изменить его до дедлайна."
                .formatted(item.getName(), MoneyUtils.formatUzs(item.getPrice()))
                : "✅ Buyurtmangiz qabul qilindi:\nTaom: %s\nNarxi: %s\nDeadlinegacha o'zgartirishingiz mumkin."
                .formatted(item.getName(), MoneyUtils.formatUzs(item.getPrice()));
    }

    public String orderUpdated(String oldMeal, String newMeal) {
        return orderUpdated(oldMeal, newMeal, UserLanguage.UZ);
    }

    public String orderUpdated(String oldMeal, String newMeal, UserLanguage language) {
        return isRu(language)
                ? "♻️ Ваш заказ обновлён:\nБыло: %s\nСтало: %s".formatted(oldMeal, newMeal)
                : "♻️ Buyurtmangiz yangilandi:\nOldin: %s\nYangi: %s".formatted(oldMeal, newMeal);
    }

    public String skippedToday() {
        return skippedToday(UserLanguage.UZ);
    }

    public String skippedToday(UserLanguage language) {
        return isRu(language) ? "✅ На сегодня отмечено как пропуск." : "✅ Bugun olmayman deb belgilandi.";
    }

    public String myOrder(UserOrder order) {
        return myOrder(order, UserLanguage.UZ);
    }

    public String myOrder(UserOrder order, UserLanguage language) {
        if (order.getStatus() == UserOrderStatus.SKIPPED) {
            return isRu(language) ? "Вы отмечены как пропуск на сегодня." : "Siz bugun olmayman deb belgilagansiz.";
        }
        if (order.getStatus() == UserOrderStatus.CANCELLED) {
            return isRu(language) ? "Ваш заказ был отменён." : "Buyurtmangiz bekor qilingan.";
        }
        return (isRu(language) ? """
                Ваш заказ:
                Блюдо: %s
                Статус: %s
                Итоговая цена: %s
                """ : """
                Sizning buyurtmangiz:
                Taom: %s
                Status: %s
                Yakuniy narx: %s
                """).formatted(
                order.getMenuItem() == null ? "N/A" : order.getMenuItem().getName(),
                order.getStatus(),
                MoneyUtils.formatUzs(order.getFinalPrice()));
    }

    public String noResponseYet() {
        return noResponseYet(UserLanguage.UZ);
    }

    public String noResponseYet(UserLanguage language) {
        return isRu(language) ? "Вы ещё не ответили." : "Siz hali javob bermagansiz.";
    }

    public String registrationRequired() {
        return registrationRequired(UserLanguage.UZ);
    }

    public String registrationRequired(UserLanguage language) {
        return isRu(language)
                ? "Пожалуйста, откройте личный чат с ботом и отправьте /start."
                : "Iltimos, bot bilan private chat ochib, /start yuboring.";
    }

    public String pendingUserCard(LunchUser user) {
        return "Pending user:\n" + user.getDisplayName() + "\nTelegram ID: " + user.getTelegramUserId();
    }

    public String pendingUserApprovalRequest(LunchUser user, UserLanguage language) {
        return isRu(language)
                ? "Новая заявка на регистрацию:\nИмя: %s\nUsername: %s\nТелефон: %s\nTelegram ID: %s\nОдобрить?"
                .formatted(user.getDisplayName(), blankOrDash(user.getUsername()), blankOrDash(user.getPhoneNumber()), user.getTelegramUserId())
                : "Yangi foydalanuvchi ro'yxatdan o'tish so'rovi:\nIsm: %s\nUsername: %s\nTelefon: %s\nTelegram ID: %s\nTasdiqlaysizmi?"
                .formatted(user.getDisplayName(), blankOrDash(user.getUsername()), blankOrDash(user.getPhoneNumber()), user.getTelegramUserId());
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
                /menu_category id | category-or--
                /menu_image id | imageUrl-or--
                /menu_photo id
                /menu_photo_cancel
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
                🍽 Buyurtma ochildi.

                Restoran: %s
                ⏰ Deadline: %s

                Buyurtma berish uchun bot lichkasiga kiring.
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
        return "Bugungi buyurtma hali ochiq. Deadline: " + session.getDeadlineAt().toLocalTime() + ".";
    }

    public String orderClosingReminder() {
        return """
                ⏰ Buyurtma yopilishiga 10 minut qoldi.

                Buyurtma bermaganlar bot lichkasidan tanlab qo'ying.
                """;
    }

    public String sessionClosedShort(long orderedCount, long skippedCount, long noResponseCount) {
        return """
                🔒 Bugungi buyurtma yopildi.

                📊 Summary:
                - Jami buyurtmalar: %d
                - Skip: %d
                - Javob bermaganlar: %d
                """.formatted(orderedCount, skippedCount, noResponseCount);
    }

    public String sessionConfirmed() {
        return "✅ Order session confirmed.";
    }

    public String paymentReceiptAcceptedForReview() {
        return "Chekingiz qabul qilindi. Adminlar ko'rib chiqadi.";
    }

    public String cashPaymentDeclared() {
        return "Naqd to'lov tanlandi. Adminlar xabardor qilindi.";
    }

    public String paymentApproved() {
        return "To'lovingiz tasdiqlandi. Rahmat.";
    }

    public String paymentRejected() {
        return "Chekingiz rad etildi. Iltimos, to'g'ri chekni qayta yuboring.";
    }

    public String uploadReceiptPrompt(UserLanguage language) {
        return isRu(language)
                ? "Отправьте фото чека в этот чат. После проверки администратор подтвердит оплату."
                : "Chek rasmini shu chatga yuboring. Admin tekshirib, to'lovni tasdiqlaydi.";
    }

    public String cashPaymentApproved() {
        return "Naqd to'lovingiz tasdiqlandi. Rahmat.";
    }

    public String noPendingPaymentForReceipt() {
        return "Sizda hozir to'lov kutilayotgan buyurtma yo'q.";
    }

    public String paymentStatus(PaymentRecordStatus status, UserLanguage language) {
        return switch (status) {
            case WAITING_PAYMENT -> isRu(language)
                    ? "Статус оплаты: ожидание оплаты."
                    : "To'lov holati: to'lov kutilmoqda.";
            case WAITING_APPROVAL, RECEIPT_SENT -> isRu(language)
                    ? "Статус оплаты: чек отправлен, ожидает подтверждения."
                    : "To'lov holati: chek yuborilgan, admin tasdig'i kutilmoqda.";
            case CASH_DECLARED -> isRu(language)
                    ? "Статус оплаты: выбран наличный расчёт."
                    : "To'lov holati: naqd to'lov tanlangan.";
            case PAID -> isRu(language)
                    ? "Статус оплаты: оплачено."
                    : "To'lov holati: to'langan.";
            case REJECTED -> isRu(language)
                    ? "Статус оплаты: чек отклонён, отправьте корректный чек."
                    : "To'lov holati: chek rad etilgan, to'g'ri chekni qayta yuboring.";
        };
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
        return accessDenied(UserLanguage.UZ);
    }

    public String accessDenied(UserLanguage language) {
        return isRu(language) ? "У вас нет доступа к этому действию." : "Sizda bu amalni bajarish huquqi yo'q.";
    }

    private boolean isRu(UserLanguage language) {
        return language == UserLanguage.RU;
    }

    private java.util.Optional<String> groupMenuByCategory(List<MenuItem> menuItems, UserLanguage language) {
        java.util.Map<String, List<MenuItem>> grouped = new java.util.LinkedHashMap<>();
        menuItems.stream()
                .sorted(java.util.Comparator
                        .comparing((MenuItem item) -> categoryLabel(item.getCategory(), language), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MenuItem::getSortOrder, java.util.Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MenuItem::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(item -> grouped.computeIfAbsent(categoryLabel(item.getCategory(), language), ignored -> new java.util.ArrayList<>()).add(item));

        return grouped.entrySet().stream()
                .map(entry -> entry.getKey() + ":\n" + IntStream.range(0, entry.getValue().size())
                        .mapToObj(index -> (index + 1) + ". " + entry.getValue().get(index).getName() + " — " + MoneyUtils.formatUzs(entry.getValue().get(index).getPrice()))
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse(""))
                .reduce((left, right) -> left + "\n\n" + right);
    }

    public String categoryLabel(String category, UserLanguage language) {
        if (category == null || category.isBlank()) {
            return isRu(language) ? "Другое" : "Boshqa";
        }
        return category;
    }

    private String blankOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
