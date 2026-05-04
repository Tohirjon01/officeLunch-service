package uz.company.lunchbot.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.company.lunchbot.dto.ApiResponse;
import uz.company.lunchbot.dto.request.CreateManualOrderRequest;
import uz.company.lunchbot.dto.request.UpdateOrderRequest;
import uz.company.lunchbot.dto.response.UserOrderResponse;
import uz.company.lunchbot.enums.PaymentStatus;
import uz.company.lunchbot.mapper.UserOrderMapper;
import uz.company.lunchbot.service.UserOrderService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserOrderController {

    private final UserOrderService userOrderService;
    private final UserOrderMapper userOrderMapper;

    @GetMapping("/order-sessions/{id}/orders")
    public ApiResponse<List<UserOrderResponse>> getOrders(@PathVariable Long id) {
        return ApiResponse.ok(userOrderService.getOrdersForSession(id).stream().map(userOrderMapper::toResponse).toList());
    }

    @PostMapping("/order-sessions/{id}/orders/manual")
    public ApiResponse<UserOrderResponse> createManual(@PathVariable Long id,
                                                       @Valid @RequestBody CreateManualOrderRequest request,
                                                       @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userOrderMapper.toResponse(userOrderService.createManualOrder(id, request, actorUserId)));
    }

    @PutMapping("/orders/{id}")
    public ApiResponse<UserOrderResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateOrderRequest request,
                                                 @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userOrderMapper.toResponse(userOrderService.updateOrder(id, request, actorUserId)));
    }

    @PostMapping("/orders/{id}/skip")
    public ApiResponse<UserOrderResponse> skip(@PathVariable Long id,
                                               @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userOrderMapper.toResponse(userOrderService.skipOrder(id, actorUserId)));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<UserOrderResponse> cancel(@PathVariable Long id,
                                                 @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userOrderMapper.toResponse(userOrderService.cancelOrder(id, actorUserId)));
    }

    @PostMapping("/orders/{id}/paid")
    public ApiResponse<UserOrderResponse> paid(@PathVariable Long id,
                                               @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userOrderMapper.toResponse(userOrderService.markPayment(id, PaymentStatus.PAID, actorUserId)));
    }

    @PostMapping("/orders/{id}/unpaid")
    public ApiResponse<UserOrderResponse> unpaid(@PathVariable Long id,
                                                 @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userOrderMapper.toResponse(userOrderService.markPayment(id, PaymentStatus.UNPAID, actorUserId)));
    }
}
