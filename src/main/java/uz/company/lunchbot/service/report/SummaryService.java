package uz.company.lunchbot.service.report;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.dto.response.MealSummaryItemResponse;
import uz.company.lunchbot.dto.response.RestaurantOrderTextResponse;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.dto.response.UserPaymentResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.repository.LunchUserRepository;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.util.MoneyUtils;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final OrderSessionService orderSessionService;
    private final UserOrderRepository userOrderRepository;
    private final LunchUserRepository lunchUserRepository;

    public SessionSummaryResponse buildSummary(Long sessionId) {
        OrderSession session = orderSessionService.getRequired(sessionId);
        List<UserOrder> orders = userOrderRepository.findAllByOrderSessionId(sessionId);
        long orderedCount = orders.stream().filter(order -> order.getStatus() == UserOrderStatus.ORDERED).count();
        long skippedCount = orders.stream().filter(order -> order.getStatus() == UserOrderStatus.SKIPPED).count();
        long noResponseCount = countNoResponse(orders);

        List<MealSummaryItemResponse> meals = buildMealSummary(orders);
        List<UserPaymentResponse> userPayments = buildUserPayments(orders);
        BigDecimal total = userPayments.stream()
                .map(UserPaymentResponse::finalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SessionSummaryResponse(
                session.getId(),
                session.getOrderDate(),
                session.getStatus(),
                orderedCount,
                skippedCount,
                noResponseCount,
                meals,
                userPayments,
                total,
                buildGroupSummaryText(session, orders, meals, userPayments, total, noResponseCount),
                buildRestaurantText(session, meals));
    }

    public RestaurantOrderTextResponse buildRestaurantTextResponse(Long sessionId) {
        SessionSummaryResponse summary = buildSummary(sessionId);
        return new RestaurantOrderTextResponse(summary.restaurantOrderText());
    }

    public String buildGroupSummaryText(Long sessionId) {
        return buildSummary(sessionId).groupSummaryText();
    }

    public String buildRestaurantText(Long sessionId) {
        return buildSummary(sessionId).restaurantOrderText();
    }

    private long countNoResponse(List<UserOrder> orders) {
        List<LunchUser> approvedUsers = lunchUserRepository.findAllByStatus(uz.company.lunchbot.enums.UserStatus.APPROVED);
        return approvedUsers.stream()
                .filter(user -> orders.stream().noneMatch(order -> order.getUser().getId().equals(user.getId())))
                .count();
    }

    private List<MealSummaryItemResponse> buildMealSummary(List<UserOrder> orders) {
        Map<String, Long> grouped = new LinkedHashMap<>();
        orders.stream()
                .filter(order -> order.getStatus() == UserOrderStatus.ORDERED && order.getMenuItem() != null)
                .sorted(Comparator.comparing(order -> order.getMenuItem().getName()))
                .forEach(order -> grouped.merge(order.getMenuItem().getName(), order.getQuantity().longValue(), Long::sum));

        return grouped.entrySet().stream()
                .map(entry -> new MealSummaryItemResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<UserPaymentResponse> buildUserPayments(List<UserOrder> orders) {
        return orders.stream()
                .filter(order -> order.getStatus() == UserOrderStatus.ORDERED && order.getMenuItem() != null)
                .sorted(Comparator.comparing(order -> order.getUser().getDisplayName()))
                .map(order -> new UserPaymentResponse(
                        order.getId(),
                        order.getUser().getDisplayName(),
                        order.getMenuItem().getName(),
                        order.getFinalPrice(),
                        order.getPaymentStatus()))
                .toList();
    }

    private String buildGroupSummaryText(OrderSession session,
                                         List<UserOrder> orders,
                                         List<MealSummaryItemResponse> meals,
                                         List<UserPaymentResponse> userPayments,
                                         BigDecimal total,
                                         long noResponseCount) {
        long orderedCount = orders.stream().filter(order -> order.getStatus() == UserOrderStatus.ORDERED).count();
        long skippedCount = orders.stream().filter(order -> order.getStatus() == UserOrderStatus.SKIPPED).count();

        String mealLines = meals.isEmpty()
                ? "No meals ordered"
                : meals.stream()
                .map(item -> item.mealName() + " - " + item.quantity())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No meals ordered");

        String paymentLines = userPayments.isEmpty()
                ? "No payments"
                : userPayments.stream()
                .map(payment -> payment.userDisplayName() + " - " + payment.mealName() + " - " + MoneyUtils.formatUzs(payment.finalPrice()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No payments");

        return """
                🔒 Lunch order closed

                Ordered: %d
                Skipped: %d
                No response: %d

                By meals:
                %s

                User payments:
                %s

                Total: %s

                Waiting for admin confirmation.
                """.formatted(orderedCount, skippedCount, noResponseCount, mealLines, paymentLines, MoneyUtils.formatUzs(total));
    }

    private String buildRestaurantText(OrderSession session, List<MealSummaryItemResponse> meals) {
        long totalMeals = meals.stream().mapToLong(MealSummaryItemResponse::quantity).sum();
        String mealLines = meals.isEmpty()
                ? "No meals ordered"
                : meals.stream()
                .map(item -> item.mealName() + " - " + item.quantity())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No meals ordered");

        String address = session.getRestaurant().getAddress() == null ? "N/A" : session.getRestaurant().getAddress();
        String phone = session.getRestaurant().getPhone() == null ? "N/A" : session.getRestaurant().getPhone();

        return """
                Assalomu alaykum.

                Today's order:

                %s

                Total: %d meals.

                Address: %s
                Phone: %s
                """.formatted(mealLines, totalMeals, address, phone);
    }
}
