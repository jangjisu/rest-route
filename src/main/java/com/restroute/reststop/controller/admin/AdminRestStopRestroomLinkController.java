package com.restroute.reststop.controller.admin;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.common.ApiResponse;
import com.restroute.reststop.controller.request.AdminRestroomLinkRequest;
import com.restroute.reststop.controller.response.AdminRestStopRestroomLinkSummaryResponse;
import com.restroute.reststop.controller.response.AdminRestroomLinkResponse;
import com.restroute.reststop.controller.response.AdminRestroomSearchResponse;
import com.restroute.reststop.service.restroom.AdminRestStopRestroomLinkService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminRestStopRestroomLinkController {

    private static final String RESTROOM_LINKED_MESSAGE = "%s 화장실 현황을 %s에 연결했습니다.";
    private static final String RESTROOM_UNLINKED_MESSAGE = "%s 화장실 현황의 연결을 해제했습니다.";

    private final AdminRestStopRestroomLinkService adminRestStopRestroomLinkService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping("/api/admin/rest-stops/restroom-links")
    public ResponseEntity<ApiResponse<List<AdminRestStopRestroomLinkSummaryResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(adminRestStopRestroomLinkService.findAll()));
    }

    /**
     * @param name 시설명 검색어(부분 일치, 대소문자 무시)
     * @param routeName 노선명 필터(정확히 일치)
     */
    @GetMapping("/api/admin/rest-stop-restrooms/search")
    public ResponseEntity<ApiResponse<List<AdminRestroomSearchResponse>>> search(
            @RequestParam(required = false) String name, @RequestParam(required = false) String routeName) {
        return ResponseEntity.ok(ApiResponse.success(adminRestStopRestroomLinkService.search(name, routeName)));
    }

    @PutMapping("/api/admin/rest-stop-restrooms/{restroomId}/link")
    public ResponseEntity<ApiResponse<AdminRestroomLinkResponse>> link(
            @PathVariable Long restroomId,
            @RequestBody AdminRestroomLinkRequest request,
            Authentication authentication) {
        AdminRestroomLinkResponse response =
                adminRestStopRestroomLinkService.link(restroomId, request.serviceAreaCode());
        adminActivityLogService.log(
                authentication,
                String.format(RESTROOM_LINKED_MESSAGE, response.sourceRestStopName(), response.restStopName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/api/admin/rest-stop-restrooms/{restroomId}/link")
    public ResponseEntity<ApiResponse<AdminRestroomLinkResponse>> unlink(
            @PathVariable Long restroomId, Authentication authentication) {
        AdminRestroomLinkResponse response = adminRestStopRestroomLinkService.unlink(restroomId);
        adminActivityLogService.log(
                authentication, String.format(RESTROOM_UNLINKED_MESSAGE, response.sourceRestStopName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
