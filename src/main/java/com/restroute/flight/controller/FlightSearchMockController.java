package com.restroute.flight.controller;

import com.restroute.flight.controller.response.FlightApiResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 개발용 모킹 컨트롤러. 요청/응답 계약은 실제 grouped_prices 연동 기준으로 확정된
 * 것이지만, 응답 데이터 자체는 아직 고정 가짜 데이터(FlightSearchMockFixture)다 — 실제
 * Travelpayouts 연동은 별도 작업이다. 프론트 개발이 운영 환경에서도 이 계약으로 붙어야 해서
 * 프로파일 제한 없이 항상 활성화된다.
 */
@RestController
@RequestMapping("/api/flights")
public class FlightSearchMockController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * 필수 값이 잘못된 경우 {@link FlightExceptionHandler}에서 처리한다.
     *
     * @param origin 출발지 IATA 코드(필수). 예: "ICN"
     * @param destination 도착지 IATA 코드(옵션, 비우면 범위 내 전체 목적지). 예: "OSA"
     * @param dateFrom 여행 날짜 범위 시작일(필수, yyyy-MM-dd, 오늘 이후만)
     * @param dateTo 여행 날짜 범위 종료일(필수, dateFrom과 최대 3개월 차이)
     * @param nights 여행 박수(복수 선택 가능, 필수, 1~90)
     * @param filter 지역권 필터(옵션). 값: JAPAN, SOUTHEAST_ASIA, GUAM_SAIPAN
     * @param dayOption 날짜 조건(옵션, 복수 선택 시 AND). 값: INCLUDE_WEEKEND, EXCLUDE_WEEKEND_INCLUDE_HOLIDAY
     * @param includeTransfer 경유 포함 여부(옵션, 기본 false=직항만)
     * @param cursor 이전 응답의 meta.nextCursor를 그대로 넘기면 그다음부터 이어서 반환
     * @param size 한 번에 받을 개수(기본 20, 최대 50)
     */
    @GetMapping("/search/mock")
    public ResponseEntity<FlightApiResponse<FlightDealSearchResponse>> searchMock(
            @RequestParam String origin,
            @RequestParam(required = false) String destination,
            @RequestParam String dateFrom,
            @RequestParam String dateTo,
            @RequestParam List<String> nights,
            @RequestParam(required = false) List<String> filter,
            @RequestParam(required = false) List<String> dayOption,
            @RequestParam(required = false) String includeTransfer,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        FlightSearchRequestValidator.ValidatedRequest validated = FlightSearchRequestValidator.validate(
                origin, destination, dateFrom, dateTo, nights, filter, dayOption, includeTransfer);
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return ResponseEntity.ok(
                FlightApiResponse.success(FlightSearchMockFixture.page(validated, cursor, boundedSize)));
    }
}
