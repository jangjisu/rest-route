package com.restroute.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.controller.response.RestStopCompareResponse;
import com.restroute.controller.response.RestStopDetailViewResponse;
import com.restroute.controller.response.RestStopItemResponse;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.compare.RestStopCompareService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rest-stops")
public class RestStopController {

    private final RestStopQueryService restStopQueryService;
    private final RestStopCompareService restStopCompareService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RestStopItemResponse>>> getRestStops() {
        List<RestStopItemResponse> restStops = restStopQueryService.findAll().stream()
                .map(RestStopItemResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(restStops));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RestStopItemResponse>>> searchRestStops(@RequestParam String name) {
        List<RestStopItemResponse> restStops = restStopQueryService.searchByName(name).stream()
                .map(RestStopItemResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(restStops));
    }

    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<RestStopCompareResponse>> compareRestStops(
            @RequestParam String serviceAreaCodeA, @RequestParam String serviceAreaCodeB) {
        RestStopCompareResponse response = restStopCompareService.compare(serviceAreaCodeA, serviceAreaCodeB);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{serviceAreaCode}")
    public ResponseEntity<ApiResponse<RestStopDetailViewResponse>> getRestStopDetail(
            @PathVariable String serviceAreaCode) {
        return restStopQueryService
                .findDetailByServiceAreaCode(serviceAreaCode)
                .map(restStop -> ResponseEntity.ok(ApiResponse.success(restStop)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.NOT_FOUND)));
    }
}
