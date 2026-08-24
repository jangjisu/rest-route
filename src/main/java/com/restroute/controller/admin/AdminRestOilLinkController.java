package com.restroute.controller.admin;

import com.restroute.common.ApiResponse;
import com.restroute.controller.request.AdminOilStationLinkRequest;
import com.restroute.controller.response.AdminOilStationLinkResponse;
import com.restroute.controller.response.AdminOilStationSearchResponse;
import com.restroute.controller.response.AdminRestOilLinkSummaryResponse;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admin.AdminRestOilLinkService;
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
public class AdminRestOilLinkController {

    private static final String OIL_STATION_LINKED_MESSAGE = "%s 주유소를 %s에 연결했습니다.";
    private static final String OIL_STATION_UNLINKED_MESSAGE = "%s 주유소의 연결을 해제했습니다.";
    private static final String OIL_STATION_OVERRIDE_CLEARED_MESSAGE = "%s 주유소를 자동 매칭으로 되돌렸습니다.";

    private final AdminRestOilLinkService adminRestOilLinkService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping("/api/admin/rest-stops/oil-links")
    public ResponseEntity<ApiResponse<List<AdminRestOilLinkSummaryResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(adminRestOilLinkService.findAll()));
    }

    /**
     * @param name 주유소명 검색어(부분 일치, 대소문자 무시). 예: "GS칼텍스"
     * @param routeName 노선명 필터(정확히 일치). 예: "경부선"
     */
    @GetMapping("/api/admin/oil-stations/search")
    public ResponseEntity<ApiResponse<List<AdminOilStationSearchResponse>>> search(
            @RequestParam(required = false) String name, @RequestParam(required = false) String routeName) {
        return ResponseEntity.ok(ApiResponse.success(adminRestOilLinkService.search(name, routeName)));
    }

    @PutMapping("/api/admin/oil-stations/{oilId}/link")
    public ResponseEntity<ApiResponse<AdminOilStationLinkResponse>> link(
            @PathVariable Long oilId, @RequestBody AdminOilStationLinkRequest request, Authentication authentication) {
        AdminOilStationLinkResponse response = adminRestOilLinkService.link(oilId, request.serviceAreaCode());
        adminActivityLogService.log(
                authentication,
                String.format(OIL_STATION_LINKED_MESSAGE, response.standardRestName(), response.restStopName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/api/admin/oil-stations/{oilId}/link")
    public ResponseEntity<ApiResponse<AdminOilStationLinkResponse>> unlink(
            @PathVariable Long oilId, Authentication authentication) {
        AdminOilStationLinkResponse response = adminRestOilLinkService.unlink(oilId);
        adminActivityLogService.log(
                authentication, String.format(OIL_STATION_UNLINKED_MESSAGE, response.standardRestName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/api/admin/oil-stations/{oilId}/override")
    public ResponseEntity<ApiResponse<AdminOilStationLinkResponse>> clearOverride(
            @PathVariable Long oilId, Authentication authentication) {
        AdminOilStationLinkResponse response = adminRestOilLinkService.clearOverride(oilId);
        adminActivityLogService.log(
                authentication, String.format(OIL_STATION_OVERRIDE_CLEARED_MESSAGE, response.standardRestName()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
