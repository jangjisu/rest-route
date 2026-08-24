package com.restroute.controller.admin;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.common.ApiResponse;
import com.restroute.controller.request.AdminRestStopUpdateRequest;
import com.restroute.controller.response.AdminRestStopEditableResponse;
import com.restroute.service.admin.AdminRestStopEditService;
import com.restroute.service.image.exception.RestStopNotFoundException;
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
@RequestMapping("/api/admin/rest-stops")
public class AdminRestStopEditController {

    private static final String CREATED_MESSAGE = "%s 휴게소를 새로 등록했습니다.";
    private static final String EDITED_MESSAGE = "%s 정보를 수정했습니다.";
    private static final String OVERRIDE_CLEARED_MESSAGE = "%s의 동기화 잠금을 해제했습니다.";

    private final AdminRestStopEditService editService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping("/{serviceAreaCode}/editable")
    public ResponseEntity<ApiResponse<AdminRestStopEditableResponse>> find(@PathVariable String serviceAreaCode) {
        AdminRestStopEditableResponse response = editService
                .findEditable(serviceAreaCode)
                .orElseThrow(() -> RestStopNotFoundException.forServiceAreaCode(serviceAreaCode));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminRestStopEditableResponse>> create(
            @RequestBody AdminRestStopUpdateRequest request, Authentication authentication) {
        AdminRestStopEditableResponse response = editService.create(request);
        adminActivityLogService.log(authentication, String.format(CREATED_MESSAGE, response.unitName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{serviceAreaCode}/editable")
    public ResponseEntity<ApiResponse<AdminRestStopEditableResponse>> update(
            @PathVariable String serviceAreaCode,
            @RequestBody AdminRestStopUpdateRequest request,
            Authentication authentication) {
        AdminRestStopEditableResponse response = editService.update(serviceAreaCode, request);
        adminActivityLogService.log(authentication, String.format(EDITED_MESSAGE, response.unitName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{serviceAreaCode}/editable/override")
    public ResponseEntity<ApiResponse<AdminRestStopEditableResponse>> clearOverride(
            @PathVariable String serviceAreaCode, Authentication authentication) {
        AdminRestStopEditableResponse response = editService.clearOverride(serviceAreaCode);
        adminActivityLogService.log(authentication, String.format(OVERRIDE_CLEARED_MESSAGE, response.unitName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
