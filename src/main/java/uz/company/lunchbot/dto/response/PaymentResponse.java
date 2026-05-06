package uz.company.lunchbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import uz.company.lunchbot.enums.PaymentMethod;
import uz.company.lunchbot.enums.PaymentRecordStatus;

public record PaymentResponse(
        Long id,
        Long userOrderId,
        Long userId,
        String userDisplayName,
        Long orderSessionId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentRecordStatus status,
        String receiptFileId,
        Long receiptMessageId,
        String adminComment,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
