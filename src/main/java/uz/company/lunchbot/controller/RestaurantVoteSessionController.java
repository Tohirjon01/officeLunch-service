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
import uz.company.lunchbot.dto.request.ChooseRestaurantWinnerRequest;
import uz.company.lunchbot.dto.request.OpenRestaurantVoteSessionRequest;
import uz.company.lunchbot.dto.response.RestaurantVoteSessionResponse;
import uz.company.lunchbot.service.RestaurantVoteSessionService;

@RestController
@RequestMapping("/api/v1/restaurant-vote-sessions")
@RequiredArgsConstructor
public class RestaurantVoteSessionController {

    private final RestaurantVoteSessionService restaurantVoteSessionService;

    @PostMapping("/open")
    public ApiResponse<RestaurantVoteSessionResponse> open(@RequestBody(required = false) OpenRestaurantVoteSessionRequest request,
                                                           @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(restaurantVoteSessionService.openSession(request, actorUserId));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<RestaurantVoteSessionResponse> close(@PathVariable Long id,
                                                            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(restaurantVoteSessionService.closeSession(id, actorUserId));
    }

    @PostMapping("/{id}/choose-winner")
    public ApiResponse<RestaurantVoteSessionResponse> chooseWinner(@PathVariable Long id,
                                                                   @Valid @RequestBody ChooseRestaurantWinnerRequest request,
                                                                   @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(restaurantVoteSessionService.chooseWinner(id, request.restaurantId(), actorUserId));
    }

    @GetMapping("/today")
    public ApiResponse<RestaurantVoteSessionResponse> getToday(@RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(restaurantVoteSessionService.getTodayResponse(actorUserId));
    }
}
