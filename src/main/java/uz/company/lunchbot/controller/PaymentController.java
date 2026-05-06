package uz.company.lunchbot.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.company.lunchbot.dto.ApiResponse;
import uz.company.lunchbot.dto.response.PaymentResponse;
import uz.company.lunchbot.dto.response.PaymentSummaryResponse;
import uz.company.lunchbot.service.PaymentService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/order-sessions/{sessionId}/payments")
    public ApiResponse<List<PaymentResponse>> getPayments(@PathVariable Long sessionId,
                                                          @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(paymentService.getPaymentsForSession(sessionId, actorUserId));
    }

    @GetMapping("/order-sessions/{sessionId}/payment-summary")
    public ApiResponse<PaymentSummaryResponse> getPaymentSummary(@PathVariable Long sessionId,
                                                                 @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(paymentService.getPaymentSummary(sessionId, actorUserId));
    }

    @PatchMapping("/payments/{paymentId}/approve")
    public ApiResponse<PaymentResponse> approve(@PathVariable Long paymentId,
                                                @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(paymentService.approvePayment(paymentId, actorUserId));
    }

    @PatchMapping("/payments/{paymentId}/reject")
    public ApiResponse<PaymentResponse> reject(@PathVariable Long paymentId,
                                               @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(paymentService.rejectPayment(paymentId, actorUserId));
    }

    @PatchMapping("/payments/{paymentId}/mark-cash-paid")
    public ApiResponse<PaymentResponse> markCashPaid(@PathVariable Long paymentId,
                                                     @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(paymentService.markCashPaid(paymentId, actorUserId));
    }
}
