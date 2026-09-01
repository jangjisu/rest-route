package com.restroute.reststop.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.reststop.controller.response.RestStopDetailViewResponse;
import com.restroute.reststop.controller.response.RestStopItemResponse;
import com.restroute.reststop.controller.response.RestStopNearbyItemResponse;
import com.restroute.reststop.service.RestStopNearbyQueryService;
import com.restroute.reststop.service.RestStopQueryService;
import com.restroute.reststop.service.dto.RestStopInterest;
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
    private final RestStopNearbyQueryService restStopNearbyQueryService;

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

    /**
     * "이름·거리로 찾기" 목록 전용 API — originLat/originLng/name/interest 전부 선택이다. 위치가
     * 없으면 거리 없이, 이름이 없으면 전체를, 관심 항목이 없으면 그 태그 없이 내려준다.
     *
     * @param originLat 내 위치 위도. 있으면 거리순으로 정렬해서 내려준다
     * @param originLng 내 위치 경도
     * @param name 휴게소명 검색어(부분 일치, 대소문자 무시)
     * @param interest 관심 있는 연료(EV/GASOLINE/DIESEL/LPG)
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<RestStopNearbyItemResponse>>> getNearbyRestStops(
            @RequestParam(required = false) Double originLat,
            @RequestParam(required = false) Double originLng,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) RestStopInterest interest) {
        return ResponseEntity.ok(
                ApiResponse.success(restStopNearbyQueryService.findNearby(originLat, originLng, name, interest)));
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
