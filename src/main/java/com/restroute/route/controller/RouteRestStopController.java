package com.restroute.route.controller;

import com.restroute.common.ApiResponse;
import com.restroute.route.controller.response.RouteRestStopListItemResponse;
import com.restroute.route.controller.response.RouteRestStopResponse;
import com.restroute.route.dto.FuelType;
import com.restroute.route.service.RouteRestStopListQueryService;
import com.restroute.route.service.RouteRestStopService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/route-rest-stops")
public class RouteRestStopController {

    private static final int DEFAULT_RADIUS_METERS = 1000;

    private final RouteRestStopService routeRestStopService;
    private final RouteRestStopListQueryService routeRestStopListQueryService;

    /**
     * @param originLat 출발지 위도. 예: 37.5665
     * @param originLng 출발지 경도. 예: 126.9780
     * @param destinationQuery 목적지 검색어(좌표 없이 이름/주소로 지오코딩할 때). 예: "부산역". destinationLat/Lng와 동시에 오면 좌표가 우선한다.
     * @param destinationLat 목적지 위도(장소 선택 등으로 좌표를 이미 아는 경우). 예: 35.1148
     * @param destinationLng 목적지 경도. 예: 129.0403
     * @param destinationName 목적지 표시명(좌표와 함께 올 때만 사용, 없으면 "목적지"로 대체). 예: "부산역"
     * @param radiusMeters 경로에서 휴게소를 포함할 반경(m). 예: 1000
     */
    @GetMapping
    public ResponseEntity<ApiResponse<RouteRestStopResponse>> getRouteRestStops(
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam(required = false) String destinationQuery,
            @RequestParam(required = false) Double destinationLat,
            @RequestParam(required = false) Double destinationLng,
            @RequestParam(required = false) String destinationName,
            @RequestParam(required = false, defaultValue = "" + DEFAULT_RADIUS_METERS) int radiusMeters) {
        RouteRestStopResponse response = routeRestStopService.findRouteRestStops(
                originLat, originLng, destinationQuery, destinationLat, destinationLng, destinationName, radiusMeters);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * finder "목적지로 추천받기" 전용. 지도 화면의 {@link #getRouteRestStops}와 별개 엔드포인트로,
     * 대안 경로·이미지 없이 첫 번째 경로의 휴게소만 출발지 기준 거리순으로 반환하고, {@code fuelType}
     * 하나만 스코프해서 유가 등급을 계산한다.
     *
     * @param originLat 출발지 위도. 예: 37.5665
     * @param originLng 출발지 경도. 예: 126.9780
     * @param destinationQuery 목적지 검색어(좌표 없이 이름/주소로 지오코딩할 때). 예: "부산역". destinationLat/Lng와 동시에 오면 좌표가 우선한다.
     * @param destinationLat 목적지 위도(장소 선택 등으로 좌표를 이미 아는 경우). 예: 35.1148
     * @param destinationLng 목적지 경도. 예: 129.0403
     * @param destinationName 목적지 표시명(좌표와 함께 올 때만 사용, 없으면 "목적지"로 대체). 예: "부산역"
     * @param radiusMeters 경로에서 휴게소를 포함할 반경(m). 예: 1000
     * @param fuelType 선택. 이 유종 하나만 비교해 유가 등급(제일 저렴/평균보다 저렴)을 계산한다. 없으면 등급은 항상 null.
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<RouteRestStopListItemResponse>>> getRouteRestStopList(
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam(required = false) String destinationQuery,
            @RequestParam(required = false) Double destinationLat,
            @RequestParam(required = false) Double destinationLng,
            @RequestParam(required = false) String destinationName,
            @RequestParam(required = false, defaultValue = "" + DEFAULT_RADIUS_METERS) int radiusMeters,
            @RequestParam(required = false) FuelType fuelType) {
        List<RouteRestStopListItemResponse> response = routeRestStopListQueryService.findRouteRestStops(
                originLat,
                originLng,
                destinationQuery,
                destinationLat,
                destinationLng,
                destinationName,
                radiusMeters,
                fuelType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
