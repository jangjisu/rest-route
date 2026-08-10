package com.restroute.flight.controller;

import com.restroute.common.ApiResponse;
import com.restroute.flight.controller.response.FlightSearchResultResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 개발용 모킹 컨트롤러.
 * 요청 파라미터를 받지 않고 조건 검증도 하지 않는다 — 화면 시안과 동일한 고정 응답만 무조건 돌려준다.
 */
@RestController
@RequestMapping("/api/flights")
public class FlightSearchMockController {

    @GetMapping("/search/mock")
    public ResponseEntity<ApiResponse<FlightSearchResultResponse>> searchMock() {
        return ResponseEntity.ok(ApiResponse.success(FlightSearchMockFixture.sampleResult()));
    }
}
