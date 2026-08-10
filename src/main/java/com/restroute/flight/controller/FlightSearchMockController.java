package com.restroute.flight.controller;

import com.restroute.common.ApiResponse;
import com.restroute.flight.controller.response.FlightSearchResultResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 개발용 모킹 컨트롤러.
 * 요청 파라미터(출발지/도착지/날짜 범위/지역권/박수/날짜 조건)는 실제 계약대로 받아서 검증하지만,
 * 응답은 아직 화면 시안과 동일한 고정값을 돌려준다 — 실제 검색/정렬 로직은 별도 작업이다.
 * local 프로파일에서만 활성화되며, 운영 환경에는 배포되지 않는다.
 */
@Slf4j
@RestController
@Profile("local")
@RequestMapping("/api/flights")
public class FlightSearchMockController {

    /**
     * @param origin 출발지 IATA 코드. 예: "ICN"
     * @param destination 도착지 IATA 코드(옵션, 비우면 범위 내 전체 목적지). 예: "OSA"
     * @param dateFrom 여행 날짜 범위 시작일. "월 범위"는 해당 월의 1일, "특정 일자"는 그 날짜를 그대로 넣는다. 예: "2026-07-01"
     * @param dateTo 여행 날짜 범위 종료일(dateFrom과 최대 3개월 차이). 예: "2026-08-31"
     * @param regions 지역권 필터(옵션, 복수 선택 가능). 값: JAPAN, SOUTHEAST_ASIA, GUAM_SAIPAN
     * @param nights 여행 박수(복수 선택 가능, 최소 1개 필수). 예: 3, 4
     * @param requireFullWeekend 여행 기간에 토요일+일요일이 모두 포함되어야 하는지
     * @param requireWeekdayHoliday 여행 기간에 주말이 아닌 공휴일이 포함되어야 하는지(requireFullWeekend와 둘 다 켜면 AND 조건)
     */
    @GetMapping("/search/mock")
    public ResponseEntity<ApiResponse<FlightSearchResultResponse>> searchMock(
            @RequestParam String origin,
            @RequestParam(required = false) String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> regions,
            @RequestParam(required = false) List<Integer> nights,
            @RequestParam(required = false, defaultValue = "false") boolean requireFullWeekend,
            @RequestParam(required = false, defaultValue = "false") boolean requireWeekdayHoliday) {
        FlightSearchRequestValidator.validate(dateFrom, dateTo, nights, regions);
        log.info(
                "Flight search mock requested. origin={}, destination={}, dateFrom={}, dateTo={}, regions={},"
                        + " nights={}, requireFullWeekend={}, requireWeekdayHoliday={}",
                origin,
                destination,
                dateFrom,
                dateTo,
                regions,
                nights,
                requireFullWeekend,
                requireWeekdayHoliday);

        return ResponseEntity.ok(ApiResponse.success(FlightSearchMockFixture.sampleResult()));
    }
}
