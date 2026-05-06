package uz.company.lunchbot.service;

import java.util.List;
import java.util.Optional;
import uz.company.lunchbot.dto.request.CreateManualOrderRequest;
import uz.company.lunchbot.dto.request.UpdateOrderRequest;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.PaymentStatus;
public interface UserOrderService {

    List<UserOrder> getOrdersForSession(Long sessionId);

    UserOrder getRequired(Long orderId);

    Optional<UserOrder> getTodayOrderForTelegramUser(Long telegramUserId);

    List<LunchUser> findNotRespondedUsers(OrderSession session);

    UserOrder placeTodayOrder(Long telegramUserId, Long menuItemId);

    UserOrder skipToday(Long telegramUserId);

    UserOrder createManualOrder(Long sessionId, CreateManualOrderRequest request, Long actorUserId);

    UserOrder updateOrder(Long orderId, UpdateOrderRequest request, Long actorUserId);

    UserOrder skipOrder(Long orderId, Long actorUserId);

    UserOrder cancelOrder(Long orderId, Long actorUserId);

    UserOrder markPayment(Long orderId, PaymentStatus paymentStatus, Long actorUserId);
}
