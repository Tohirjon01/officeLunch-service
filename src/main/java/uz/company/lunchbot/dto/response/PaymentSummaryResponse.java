package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PaymentSummaryResponse(
        Long orderSessionId,
        BigDecimal totalExpectedAmount,
        BigDecimal totalPaidAmount,
        BigDecimal totalUnpaidAmount,
        List<PaymentResponse> waitingPayments,
        List<PaymentResponse> receiptSentPayments,
        List<PaymentResponse> cashDeclaredPayments,
        List<PaymentResponse> paidPayments,
        List<PaymentResponse> rejectedPayments
) {
}
