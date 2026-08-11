package com.restroute.flight.controller;

import com.restroute.flight.controller.response.FlightApiResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 개발용 모킹 컨트롤러. 요청 파라미터는 실제 grouped_prices 호출에 필요한
 * 값(origin/destination/기간/박수) 기준으로 검증하지만, 응답 데이터 자체는 아직 고정
 * 가짜 데이터(FlightSearchMockFixture)다 — 실제 Travelpayouts 연동은 별도 작업이다.
 * local 프로파일에서만 활성화되며, 운영 환경에는 배포되지 않는다.
 */
@RestController
@Profile("local")
@RequestMapping("/api/flights")
public class FlightSearchMockController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * @param origin 출발지 IATA 코드(필수). 예: "ICN"
     * @param destination 도착지 IATA 코드(옵션, 비우면 범위 내 전체 목적지). 예: "OSA"
     * @param dateFrom 여행 날짜 범위 시작일(필수, yyyy-MM-dd, 오늘 이후만)
     * @param dateTo 여행 날짜 범위 종료일(필수, dateFrom과 최대 3개월 차이)
     * @param nights 여행 박수(복수 선택 가능, 최소 1개 필수)
     * @param regions 지역권 필터(옵션). 값: JAPAN, SOUTHEAST_ASIA, GUAM_SAIPAN
     * @param cursor 이전 응답의 meta.nextCursor를 그대로 넘기면 그다음부터 이어서 반환
     * @param size 한 번에 받을 개수(기본 20, 최대 50)
     */
    @GetMapping("/search/mock")
    public ResponseEntity<FlightApiResponse<FlightDealSearchResponse>> searchMock(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) List<String> nights,
            @RequestParam(required = false) List<String> regions,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        FlightSearchRequestValidator.ValidatedRequest validated =
                FlightSearchRequestValidator.validate(origin, destination, dateFrom, dateTo, nights, regions);
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return ResponseEntity.ok(
                FlightApiResponse.success(FlightSearchMockFixture.page(validated, cursor, boundedSize)));
    }
}
