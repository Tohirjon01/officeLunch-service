package uz.company.lunchbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.company.lunchbot.dto.ApiResponse;
import uz.company.lunchbot.dto.request.ExtendOrderSessionRequest;
import uz.company.lunchbot.dto.request.MoneyAmountRequest;
import uz.company.lunchbot.dto.request.OpenOrderSessionRequest;
import uz.company.lunchbot.dto.response.OrderSessionResponse;
import uz.company.lunchbot.dto.response.RestaurantOrderTextResponse;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.mapper.OrderSessionMapper;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.report.SummaryService;

@RestController
@RequestMapping("/api/v1/order-sessions")
@RequiredArgsConstructor
public class OrderSessionController {

    private final OrderSessionService orderSessionService;
    private final OrderSessionMapper orderSessionMapper;
    private final SummaryService summaryService;

    @PostMapping("/open")
    public ApiResponse<OrderSessionResponse> open(@RequestBody(required = false) OpenOrderSessionRequest request,
                                                  @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.openSession(request, actorUserId)));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<OrderSessionResponse> close(@PathVariable Long id,
                                                   @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.closeSession(id, actorUserId)));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderSessionResponse> confirm(@PathVariable Long id,
                                                     @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.confirmSession(id, actorUserId)));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderSessionResponse> cancel(@PathVariable Long id,
                                                    @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.cancelSession(id, actorUserId)));
    }

    @PostMapping("/{id}/extend")
    public ApiResponse<OrderSessionResponse> extend(@PathVariable Long id,
                                                    @Valid @RequestBody ExtendOrderSessionRequest request,
                                                    @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.extendSession(id, request.minutes(), actorUserId)));
    }

    @GetMapping("/today")
    public ApiResponse<OrderSessionResponse> getToday() {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException("Today's order session not found"))));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderSessionResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.getRequired(id)));
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<SessionSummaryResponse> getSummary(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.buildSummary(id));
    }

    @GetMapping("/{id}/restaurant-text")
    public ApiResponse<RestaurantOrderTextResponse> getRestaurantText(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.buildRestaurantTextResponse(id));
    }

    @PostMapping("/{id}/delivery")
    public ApiResponse<OrderSessionResponse> updateDelivery(@PathVariable Long id,
                                                            @Valid @RequestBody MoneyAmountRequest request,
                                                            @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.updateDeliveryPrice(id, request.amount(), actorUserId)));
    }

    @PostMapping("/{id}/container")
    public ApiResponse<OrderSessionResponse> updateContainer(@PathVariable Long id,
                                                             @Valid @RequestBody MoneyAmountRequest request,
                                                             @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(orderSessionMapper.toResponse(orderSessionService.updateContainerPrice(id, request.amount(), actorUserId)));
    }
}
