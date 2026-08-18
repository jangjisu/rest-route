package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * RANGE 검색 하나를 실제로 몇 번, 어떤 파라미터로 Travelpayouts에 물어볼지 정한다.
 *
 * <p>destination(직접 지정/sector의 국가들/생략)과 개월 수(dateFrom~dateTo가 걸치는 달)는
 * 그대로 곱해지고 줄일 수 없지만, nights는 "요청한 값 각각 정확히"(풍부한 결과) 또는
 * "min~max 범위 하나로"(호출 적음) 둘 중 고를 수 있다 — 전체 호출 수가
 * {@value #MAX_FANOUT_CALLS}를 넘으면 자동으로 범위 모드로 낮춘다.
 */
final class FlightRangeSearchPlanner {

    private static final int MAX_FANOUT_CALLS = 20;
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private FlightRangeSearchPlanner() {}

    static FlightRangeSearchPlan plan(FlightSearchRequestDto request) {
        List<String> destinations = destinationsOf(request);
        List<String> months = monthsOf(request.parsedDateFrom(), request.parsedDateTo());
        int destinationCount = Math.max(1, destinations.size());
        List<Integer> nights = request.parsedNights();

        if (!CollectionUtils.isEmpty(request.nights())) {
            List<FlightRangeSearchPlan.NightsWindow> exactWindows = nights.stream()
                    .distinct()
                    .map(n -> new FlightRangeSearchPlan.NightsWindow(n, n))
                    .toList();
            if ((long) destinationCount * months.size() * exactWindows.size() <= MAX_FANOUT_CALLS) {
                return new FlightRangeSearchPlan(destinations, months, exactWindows);
            }
        }

        FlightRangeSearchPlan.NightsWindow rangeWindow =
                new FlightRangeSearchPlan.NightsWindow(Collections.min(nights), Collections.max(nights));
        return new FlightRangeSearchPlan(destinations, months, List.of(rangeWindow));
    }

    /** destination을 직접 지정했으면 그거 하나, sector면 그 sector들의 국가 목록, 둘 다 없으면 빈 목록(생략). */
    private static List<String> destinationsOf(FlightSearchRequestDto request) {
        if (StringUtils.hasText(request.destination())) {
            return List.of(request.destination());
        }
        return FlightSectorCountries.countriesOf(request.sector());
    }

    private static List<String> monthsOf(LocalDate dateFrom, LocalDate dateTo) {
        List<String> months = new ArrayList<>();
        YearMonth current = YearMonth.from(dateFrom);
        YearMonth end = YearMonth.from(dateTo);
        while (!current.isAfter(end)) {
            months.add(current.format(YEAR_MONTH_FORMAT));
            current = current.plusMonths(1);
        }
        return months;
    }
}
