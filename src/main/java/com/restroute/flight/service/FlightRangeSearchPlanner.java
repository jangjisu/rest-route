package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.util.CollectionUtils;

/**
 * RANGE 검색 하나를 실제로 몇 번, 어떤 파라미터로 Travelpayouts에 물어볼지 정한다.
 *
 * <p>실제 호출 횟수는 세 축의 곱이다: {@code destinations.size() × months.size() ×
 * nightsWindows.size()}(destination이 생략이면 1로 친다). 이 중 destination(직접 지정/sector의
 * 국가들/생략)과 개월 수(dateFrom~dateTo가 걸치는 달력상 월)는 검색 조건 자체가 정하는 값이라
 * 줄일 수 없다 — grouped_prices가 destination 하나·달 하나 단위로만 응답을 주기 때문이다.
 *
 * <p>줄일 수 있는 건 nights뿐이다. 사용자가 nights를 여러 개 골랐을 때:
 *
 * <ul>
 *   <li><b>개별 모드</b> — 값 하나하나를 정확한 창({@code min=max=그값})으로 따로 조회한다.
 *       "3박짜리는 얼마, 4박짜리는 얼마"를 각각 보여줄 수 있어 결과가 풍부하지만, nights 개수만큼
 *       호출이 늘어난다.
 *   <li><b>범위 모드</b> — nights 전체를 {@code min~max} 창 하나로 뭉쳐서 한 번만 조회한다. 호출은
 *       destination×months만큼만 나가지만, 그 안에서 제일 싼 조합 하나만 대표로 온다(정확히 몇
 *       박인지는 응답의 departure_at~return_at 차이로만 알 수 있다).
 * </ul>
 *
 * <p>전체 호출 수가 {@value #MAX_FANOUT_CALLS}를 넘지 않는 한 개별 모드를 우선 시도하고, 넘으면
 * 범위 모드로 자동으로 낮춘다. 예시(1개월 기준):
 *
 * <pre>
 * sector=JAPAN(1개국), nights=[3,4,5]        → 1×1×3=3  ≤20 → 개별 모드, 3번 호출
 * sector 4개 전부(9개국), nights=[3,4]       → 9×1×2=18 ≤20 → 개별 모드, 18번 호출
 * sector 4개 전부(9개국), nights=[3,4,5]     → 9×1×3=27 >20 → 범위 모드로 낮춤, 9번 호출
 * </pre>
 *
 * <p>nights를 아예 안 준 경우({@link FlightSearchRequestDto#parsedNights()}가 1~기간일수
 * 전체로 자동 확장된 경우)는 개별 모드를 시도조차 하지 않고 바로 범위 모드로 간다 — 자동 확장된
 * 값은 최대 90개까지 갈 수 있어 개별로 쪼개는 게 애초에 말이 안 되기 때문이다.
 */
final class FlightRangeSearchPlanner {

    private static final int MAX_FANOUT_CALLS = 20;
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private FlightRangeSearchPlanner() {}

    static FlightRangeSearchPlan plan(FlightSearchRequestDto request) {
        List<String> destinations = FlightSearchDestinations.resolve(request);
        List<String> months = monthsOf(request.parsedDateFrom(), request.parsedDateTo());
        int destinationCount = Math.max(1, destinations.size());

        boolean nightsExplicitlyGiven = !CollectionUtils.isEmpty(request.nights());
        if (nightsExplicitlyGiven) {
            List<FlightRangeSearchPlan.NightsWindow> exactWindows = exactWindowsOf(request.parsedNights());
            if (fitsWithinBudget(destinationCount, months.size(), exactWindows.size())) {
                return new FlightRangeSearchPlan(destinations, months, exactWindows);
            }
        }

        return new FlightRangeSearchPlan(destinations, months, List.of(rangeWindowOf(request.parsedNights())));
    }

    /** dateFrom~dateTo가 걸치는 달력상 월을 순서대로 "yyyy-MM"로 나열한다(양 끝 달 포함). */
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

    /** nights 값 하나하나를 min=max=그값인 정확한 창으로 만든다 — 같은 값이 중복되면 한 번만 남긴다. */
    private static List<FlightRangeSearchPlan.NightsWindow> exactWindowsOf(List<Integer> nights) {
        return nights.stream()
                .distinct()
                .map(n -> new FlightRangeSearchPlan.NightsWindow(n, n))
                .toList();
    }

    /** nights 전체를 최솟값~최댓값 창 하나로 뭉갠다. */
    private static FlightRangeSearchPlan.NightsWindow rangeWindowOf(List<Integer> nights) {
        return new FlightRangeSearchPlan.NightsWindow(Collections.min(nights), Collections.max(nights));
    }

    private static boolean fitsWithinBudget(int destinationCount, int monthCount, int nightsWindowCount) {
        return (long) destinationCount * monthCount * nightsWindowCount <= MAX_FANOUT_CALLS;
    }
}
