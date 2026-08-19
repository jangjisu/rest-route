package com.restroute.flight.service.util;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.service.dto.FlightRangeSearchPlan;
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
 * nightsWindows.size()}(destination이 생략이면 1로 친다). 개월 수(dateFrom~dateTo가 걸치는
 * 달력상 월)는 검색 조건 자체가 정하는 값이라 줄일 수 없다 — grouped_prices가 달 하나 단위로만
 * 응답을 주기 때문이다. destination과 nights는 예산이 부족하면 아래처럼 단계적으로 낮춘다.
 *
 * <p><b>1단계 — nights 축.</b> 사용자가 nights를 여러 개 골랐을 때:
 *
 * <ul>
 *   <li><b>개별 모드</b> — 값 하나하나를 정확한 창({@code min=max=그값})으로 따로 조회한다.
 *       "3박짜리는 얼마, 4박짜리는 얼마"를 각각 보여줄 수 있어 결과가 풍부하지만, nights 개수만큼
 *       호출이 늘어난다.
 *   <li><b>범위 모드</b> — nights 전체를 {@code min~max} 창 하나로 뭉쳐서 한 번만 조회한다.
 * </ul>
 *
 * <p>nights를 아예 안 준 경우(자동 확장된 경우)는 개별 모드를 시도조차 하지 않고 바로 범위
 * 모드로 간다 — 자동 확장된 값은 최대 90개까지 갈 수 있어 개별로 쪼개는 게 애초에 말이 안 되기
 * 때문이다.
 *
 * <p><b>2단계 — destination 축.</b> sector로 여러 국가가 잡혔으면(직접 지정 destination은
 * 이미 1개라 해당 없음), 국가별 조회에 "전체"(destination 생략) 조회를 하나 더 얹어서 함께
 * 보여준다 — 국가별 조회 개수(N) + 전체(1) = N+1로 예산을 다시 확인해서, 그래도 넘지 않으면
 * 국가별+전체를 함께, 넘으면 국가별 조회를 포기하고 전체 하나만 한다.
 *
 * <p>이 순서 덕분에 최종 호출 수는 항상 {@value #MAX_FANOUT_CALLS}를 넘지 않는다 — 전체만 하는
 * 경우는 destination 축이 1이라 {@code 1 × months × nightsWindows}인데, nightsWindows는 이미
 * 1단계에서 원래 destination 개수 기준으로 예산 안에 들도록 정해졌기 때문이다. 예시(1개월
 * 기준):
 *
 * <pre>
 * sector=JAPAN(1개국), nights=[3,4,5]         → 국가별 1×1×3=3, +전체 2×1×3=6 ≤20 → 국가별+전체
 * sector 4개 전부(9개국), nights=[3,4]        → 국가별 9×1×2=18 ≤20 → nights는 개별 유지
 *                                                +전체 10×1×2=20 ≤20 → 국가별+전체
 * sector 4개 전부(9개국), nights=[3,4,5]      → 국가별 9×1×3=27 >20 → nights 범위 모드로 낮춤(9×1×1=9)
 *                                                +전체 10×1×1=10 ≤20 → 국가별+전체
 * sector 4개 전부(9개국), 3개월 걸침, nights=[3] → nights 유지(9×3×1=27>20→범위 9×3×1=27,
 *                                                여전히 초과) → 국가별 포기, 전체만(1×3×1=3)
 * </pre>
 */
public final class FlightRangeSearchPlanner {

    private static final int MAX_FANOUT_CALLS = 20;
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private FlightRangeSearchPlanner() {}

    public static FlightRangeSearchPlan plan(FlightSearchRequestDto request) {
        List<String> destinations = FlightSearchDestinations.resolve(request);
        List<String> months = monthsOf(request.parsedDateFrom(), request.parsedDateTo());
        int destinationCount = Math.max(1, destinations.size());

        List<FlightRangeSearchPlan.NightsWindow> nightsWindows;
        boolean nightsExplicitlyGiven = !CollectionUtils.isEmpty(request.nights());
        if (nightsExplicitlyGiven) {
            List<FlightRangeSearchPlan.NightsWindow> exactWindows = exactWindowsOf(request.parsedNights());
            nightsWindows = fitsWithinBudget(destinationCount, months.size(), exactWindows.size())
                    ? exactWindows
                    : List.of(rangeWindowOf(request.parsedNights()));
        } else {
            nightsWindows = List.of(rangeWindowOf(request.parsedNights()));
        }

        List<String> resolvedDestinations = FlightSearchDestinations.isSectorBased(request)
                ? withAggregateIfBudgetAllows(destinations, months.size(), nightsWindows.size())
                : destinations;
        return new FlightRangeSearchPlan(resolvedDestinations, months, nightsWindows);
    }

    /**
     * destination을 직접 지정한 경우는 호출하지 않는다(사용자가 정확히 그 하나만 원한 것이라
     * "전체"를 덧붙이면 안 된다 — 호출부에서 이미 걸러진다). sector로 국가가 잡혔고 그 목록이
     * 비어있지 않으면, "국가별 + 전체"(destination null 하나 추가)가 예산 안에 들면 그렇게 하고,
     * 넘으면 국가별 조회를 포기하고 "전체"(빈 목록, {@link FlightSearchDestinations#paddedForCalls}가
     * null 하나로 채워준다) 하나만 한다. sector조차 없으면(destinations 비어있음) 이미 "전체"이니
     * 그대로 둔다.
     */
    private static List<String> withAggregateIfBudgetAllows(
            List<String> destinations, int monthCount, int nightsWindowCount) {
        if (destinations.isEmpty()) {
            return destinations;
        }
        int detailedCount = destinations.size() + 1;
        if (!fitsWithinBudget(detailedCount, monthCount, nightsWindowCount)) {
            return List.of();
        }
        return FlightSearchDestinations.withAggregate(destinations);
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
