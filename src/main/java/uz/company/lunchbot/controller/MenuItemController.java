package uz.company.lunchbot.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.company.lunchbot.dto.ApiResponse;
import uz.company.lunchbot.dto.request.CreateMenuItemRequest;
import uz.company.lunchbot.dto.request.MenuItemContainerSettingsRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemCategoryRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemImageRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.dto.response.MenuItemResponse;
import uz.company.lunchbot.mapper.MenuItemMapper;
import uz.company.lunchbot.service.MenuItemService;

@RestController
@RequestMapping("/api/v1/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final MenuItemMapper menuItemMapper;

    @GetMapping
    public ApiResponse<List<MenuItemResponse>> getMenuItems(@RequestParam(required = false) Long restaurantId) {
        return ApiResponse.ok(menuItemService.getAll(restaurantId).stream().map(menuItemMapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuItemResponse> getMenuItem(@PathVariable Long id) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.getRequired(id)));
    }

    @PostMapping
    public ApiResponse<MenuItemResponse> create(@Valid @RequestBody CreateMenuItemRequest request,
                                                @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.create(request, actorUserId)));
    }

    @PutMapping("/{id}")
    public ApiResponse<MenuItemResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateMenuItemRequest request,
                                                @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.update(id, request, actorUserId)));
    }

    @PatchMapping("/{id}/category")
    public ApiResponse<MenuItemResponse> updateCategory(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateMenuItemCategoryRequest request,
                                                        @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.updateCategory(id, request, actorUserId)));
    }

    @PatchMapping("/{id}/image")
    public ApiResponse<MenuItemResponse> updateImage(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateMenuItemImageRequest request,
                                                     @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.updateImage(id, request, actorUserId)));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<MenuItemResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateStatusRequest request,
                                                      @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.updateStatus(id, request, actorUserId)));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<MenuItemResponse> activate(@PathVariable Long id,
                                                  @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.updateStatus(id, new UpdateStatusRequest(true), actorUserId)));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<MenuItemResponse> deactivate(@PathVariable Long id,
                                                    @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.updateStatus(id, new UpdateStatusRequest(false), actorUserId)));
    }

    @PatchMapping("/{id}/container")
    public ApiResponse<MenuItemResponse> updateContainer(@PathVariable Long id,
                                                         @Valid @RequestBody MenuItemContainerSettingsRequest request,
                                                         @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.updateContainerSettings(id, request, actorUserId)));
    }

    @DeleteMapping("/{id}/container-override")
    public ApiResponse<MenuItemResponse> removeContainerOverride(@PathVariable Long id,
                                                                 @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuItemMapper.toResponse(menuItemService.removeContainerOverride(id, actorUserId)));
    }
}
