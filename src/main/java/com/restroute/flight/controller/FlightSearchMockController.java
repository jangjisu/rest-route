package com.restroute.flight.controller;

import com.restroute.common.ApiResponse;
import com.restroute.flight.controller.response.FlightSearchResultResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 개발용 모킹 컨트롤러.
 * 실제 검색 조건(출발지/목적지/기간 범위/며칠/날짜 조건) 반영 로직과 응답 정렬 기준은
 * 아직 확정 전이라, 화면 시안과 동일한 형태의 고정 응답만 우선 제공한다.
 * local 프로파일에서만 활성화되며, 운영 환경에는 배포되지 않는다.
 */
@RestController
@Profile("local")
@RequestMapping("/api/flights")
public class FlightSearchMockController {

    @GetMapping("/search/mock")
    public ResponseEntity<ApiResponse<FlightSearchResultResponse>> searchMock() {
        return ResponseEntity.ok(ApiResponse.success(FlightSearchMockFixture.sampleResult()));
    }
}
