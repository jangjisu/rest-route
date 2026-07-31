package com.restroute.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.controller.response.RestStopDetailViewResponse;
import com.restroute.controller.response.RestStopItemResponse;
import com.restroute.service.RestStopQueryService;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<RestStopItemResponse>>> getRestStops() {
        List<RestStopItemResponse> restStops = restStopQueryService.findAll().stream()
                .map(RestStopItemResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(restStops));
    }

    /**
     * @param name 휴게소명 검색어(부분 일치, 대소문자 무시). 예: "안성"
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RestStopItemResponse>>> searchRestStops(@RequestParam String name) {
        List<RestStopItemResponse> restStops = restStopQueryService.searchByName(name).stream()
                .map(RestStopItemResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(restStops));
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
