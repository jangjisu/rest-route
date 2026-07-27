package com.restroute.controller;

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

    private final AdminRestOilLinkService adminRestOilLinkService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping("/api/admin/rest-stops/oil-links")
    public ResponseEntity<ApiResponse<List<AdminRestOilLinkSummaryResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(adminRestOilLinkService.findAll()));
    }

    @GetMapping("/api/admin/oil-stations/search")
    public ResponseEntity<ApiResponse<List<AdminOilStationSearchResponse>>> search(
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(ApiResponse.success(adminRestOilLinkService.search(name)));
    }

    @PutMapping("/api/admin/oil-stations/{oilId}/link")
    public ResponseEntity<ApiResponse<AdminOilStationLinkResponse>> link(
            @PathVariable Long oilId, @RequestBody AdminOilStationLinkRequest request, Authentication authentication) {
        AdminOilStationLinkResponse response = adminRestOilLinkService.link(oilId, request.serviceAreaCode());
        adminActivityLogService.logOilStationLinked(
                authentication, response.standardRestName(), response.restStopName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/api/admin/oil-stations/{oilId}/link")
    public ResponseEntity<ApiResponse<AdminOilStationLinkResponse>> unlink(
            @PathVariable Long oilId, Authentication authentication) {
        AdminOilStationLinkResponse response = adminRestOilLinkService.unlink(oilId);
        adminActivityLogService.logOilStationUnlinked(authentication, response.standardRestName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/api/admin/oil-stations/{oilId}/override")
    public ResponseEntity<ApiResponse<AdminOilStationLinkResponse>> clearOverride(
            @PathVariable Long oilId, Authentication authentication) {
        AdminOilStationLinkResponse response = adminRestOilLinkService.clearOverride(oilId);
        adminActivityLogService.logOilStationOverrideCleared(authentication, response.standardRestName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
