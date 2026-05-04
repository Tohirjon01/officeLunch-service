package uz.company.lunchbot.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.company.lunchbot.dto.ApiResponse;
import uz.company.lunchbot.dto.request.CreateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.dto.response.RestaurantResponse;
import uz.company.lunchbot.mapper.RestaurantMapper;
import uz.company.lunchbot.service.RestaurantService;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RestaurantMapper restaurantMapper;

    @GetMapping
    public ApiResponse<List<RestaurantResponse>> getRestaurants() {
        return ApiResponse.ok(restaurantService.getAll().stream().map(restaurantMapper::toResponse).toList());
    }

    @PostMapping
    public ApiResponse<RestaurantResponse> create(@Valid @RequestBody CreateRestaurantRequest request,
                                                  @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(restaurantMapper.toResponse(restaurantService.create(request, actorUserId)));
    }

    @PutMapping("/{id}")
    public ApiResponse<RestaurantResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateRestaurantRequest request,
                                                  @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(restaurantMapper.toResponse(restaurantService.update(id, request, actorUserId)));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RestaurantResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateStatusRequest request,
                                                        @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(restaurantMapper.toResponse(restaurantService.updateStatus(id, request, actorUserId)));
    }
}
