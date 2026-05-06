package uz.company.lunchbot.service;

import java.util.List;
import uz.company.lunchbot.dto.response.PaymentResponse;
import uz.company.lunchbot.dto.response.PaymentSummaryResponse;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Payment;

public interface PaymentService {

    Payment getRequired(Long id);

    void initializePaymentsForClosedSession(OrderSession session);

    void submitReceipt(Long telegramUserId, String receiptFileId, Long receiptMessageId);

    void declareCashPayment(Long telegramUserId, Long paymentId);

    PaymentResponse approvePayment(Long paymentId, Long actorUserId);

    PaymentResponse rejectPayment(Long paymentId, Long actorUserId);

    PaymentResponse markCashPaid(Long paymentId, Long actorUserId);

    List<PaymentResponse> getPaymentsForSession(Long sessionId, Long actorUserId);

    PaymentSummaryResponse getPaymentSummary(Long sessionId, Long actorUserId);

    String buildPaymentSummaryText(Long sessionId, Long actorUserId);

    String getUserPaymentStatusText(Long telegramUserId, Long paymentId);
}
