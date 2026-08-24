package com.restroute.reststopcontent.controller.admin;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.common.ApiResponse;
import com.restroute.controller.request.AdminRestFoodRequest;
import com.restroute.reststopcontent.controller.response.AdminRestFoodResponse;
import com.restroute.reststopcontent.service.admin.AdminRestFoodService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/rest-stops/{serviceAreaCode}/foods")
public class AdminRestFoodController {

    private static final String CUSTOM_FOOD_ADDED_MESSAGE = "%s 메뉴를 추가했습니다.";
    private static final String CUSTOM_FOOD_EDITED_MESSAGE = "%s 메뉴를 수정했습니다.";
    private static final String CUSTOM_FOOD_OVERRIDE_CLEARED_MESSAGE = "%s 메뉴의 동기화 잠금을 해제했습니다.";
    private static final String CUSTOM_FOOD_DELETED_MESSAGE = "메뉴(%d)를 삭제했습니다.";

    private final AdminRestFoodService adminRestFoodService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminRestFoodResponse>>> findByServiceAreaCode(
            @PathVariable String serviceAreaCode) {
        return ResponseEntity.ok(ApiResponse.success(adminRestFoodService.findByServiceAreaCode(serviceAreaCode)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminRestFoodResponse>> create(
            @PathVariable String serviceAreaCode,
            @RequestBody AdminRestFoodRequest request,
            Authentication authentication) {
        AdminRestFoodResponse response = adminRestFoodService.create(serviceAreaCode, request);
        adminActivityLogService.log(authentication, String.format(CUSTOM_FOOD_ADDED_MESSAGE, response.foodName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{foodId}")
    public ResponseEntity<ApiResponse<AdminRestFoodResponse>> update(
            @PathVariable String serviceAreaCode,
            @PathVariable Long foodId,
            @RequestBody AdminRestFoodRequest request,
            Authentication authentication) {
        AdminRestFoodResponse response = adminRestFoodService.update(serviceAreaCode, foodId, request);
        adminActivityLogService.log(authentication, String.format(CUSTOM_FOOD_EDITED_MESSAGE, response.foodName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{foodId}/override")
    public ResponseEntity<ApiResponse<AdminRestFoodResponse>> clearOverride(
            @PathVariable String serviceAreaCode, @PathVariable Long foodId, Authentication authentication) {
        AdminRestFoodResponse response = adminRestFoodService.clearOverride(serviceAreaCode, foodId);
        adminActivityLogService.log(
                authentication, String.format(CUSTOM_FOOD_OVERRIDE_CLEARED_MESSAGE, response.foodName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{foodId}")
    public ResponseEntity<Void> delete(
            @PathVariable String serviceAreaCode, @PathVariable Long foodId, Authentication authentication) {
        adminRestFoodService.delete(serviceAreaCode, foodId);
        adminActivityLogService.log(authentication, String.format(CUSTOM_FOOD_DELETED_MESSAGE, foodId));
        return ResponseEntity.noContent().build();
    }
}
