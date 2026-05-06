package uz.company.lunchbot.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import uz.company.lunchbot.dto.request.OpenOrderSessionRequest;
import uz.company.lunchbot.dto.request.RecalculateSessionRequest;
import uz.company.lunchbot.dto.response.SessionRecalculationResponse;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.enums.RecalculationMode;
public interface OrderSessionService {

    OrderSession getRequired(Long id);

    Optional<OrderSession> getTodaySession();

    Optional<OrderSession> getTodayOpenSession();

    OrderSession getActiveOrderingSession();

    OrderSession openSession(OpenOrderSessionRequest request, Long actorUserId);

    OrderSession openTodayDefaultSessionIfAbsent();

    OrderSession openTodaySessionForRestaurantIfAbsent(Long restaurantId, Long actorUserId);

    OrderSession closeSession(Long sessionId, Long actorUserId);

    OrderSession confirmSession(Long sessionId, Long actorUserId);

    OrderSession cancelSession(Long sessionId, Long actorUserId);

    OrderSession extendSession(Long sessionId, int minutes, Long actorUserId);

    OrderSession reduceDeadline(Long sessionId, int minutes, Long actorUserId);

    OrderSession updateDeliveryPrice(Long sessionId, BigDecimal amount, Long actorUserId);

    SessionRecalculationResponse recalculateSession(Long sessionId, RecalculateSessionRequest request, Long actorUserId);

    void ensureUserCanPlaceOrder(OrderSession session);

    void ensureAdminCanEditOrders(OrderSession session);
}
