package uz.company.lunchbot.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.company.lunchbot.dto.ApiResponse;
import uz.company.lunchbot.dto.request.ChangeUserRoleRequest;
import uz.company.lunchbot.dto.response.UserResponse;
import uz.company.lunchbot.mapper.UserMapper;
import uz.company.lunchbot.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.ok(userService.getAll().stream().map(userMapper::toResponse).toList());
    }

    @GetMapping("/pending")
    public ApiResponse<List<UserResponse>> getPendingUsers() {
        return ApiResponse.ok(userService.getPendingUsers().stream().map(userMapper::toResponse).toList());
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<UserResponse> approve(@PathVariable Long id,
                                             @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userMapper.toResponse(userService.approve(id, actorUserId)));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<UserResponse> reject(@PathVariable Long id,
                                            @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userMapper.toResponse(userService.reject(id, actorUserId)));
    }

    @PostMapping("/{id}/block")
    public ApiResponse<UserResponse> block(@PathVariable Long id,
                                           @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userMapper.toResponse(userService.block(id, actorUserId)));
    }

    @PostMapping("/{id}/role")
    public ApiResponse<UserResponse> changeRole(@PathVariable Long id,
                                                @RequestBody ChangeUserRoleRequest request,
                                                @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
        return ApiResponse.ok(userMapper.toResponse(userService.changeRole(id, request.role(), actorUserId)));
    }
}
