package com.restroute.flight.controller;

import com.restroute.flight.controller.response.FlightApiResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 개발용 모킹 컨트롤러.
 * 검색 조건은 아직 안 받고, 커서 기반 페이지네이션만 고정 가짜 데이터(FlightSearchMockFixture)로
 * 먼저 검증한다 — 실제 검색 조건/비즈니스 로직은 별도 작업이다.
 * local 프로파일에서만 활성화되며, 운영 환경에는 배포되지 않는다.
 */
@RestController
@Profile("local")
@RequestMapping("/api/flights")
public class FlightSearchMockController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * @param cursor 이전 응답의 meta.nextCursor를 그대로 넘기면 그다음부터 이어서 반환. 없으면 처음부터.
     * @param size 한 번에 받을 개수(기본 20, 최대 50)
     */
    @GetMapping("/search/mock")
    public ResponseEntity<FlightApiResponse<FlightDealSearchResponse>> searchMock(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return ResponseEntity.ok(FlightApiResponse.success(FlightSearchMockFixture.page(cursor, boundedSize)));
    }
}
