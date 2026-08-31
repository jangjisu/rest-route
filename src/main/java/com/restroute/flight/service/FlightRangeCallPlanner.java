package com.restroute.flight.service;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.service.util.FlightSearchDestinations;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * RANGE 검색 하나를 실제로 몇 번, 어떤 파라미터로 Travelpayouts에 물어볼지 정하고, 그 호출들을
 * 바로 실행 가능한 {@link Callable} 목록으로 만든다. 실행(병렬 호출·dedup)은 {@link
 * com.restroute.flight.service.util.FlightParallelPriceCalls}가 RANGE/FIXED 공통으로 한다.
 *
 * <p>호출 횟수는 {@code destinations × months × nightsWindows}의 곱이고, 개월 수는 검색 조건이
 * 정하는 값이라 줄일 수 없다(grouped_prices가 달 단위로만 응답한다). 예산이 모자라면 nights 축을
 * 먼저 범위 모드로 낮추고, 그다음 destination 축에서 국가별 조회를 포기한다 — 그래서 최종 호출
 * 수는 항상 {@link FlightSearchDestinations#MAX_FANOUT_CALLS} 이하다. 단계별 규칙과 예시는
 * {@code docs/domain/flight.md} "정책과 불변 조건" 참고.
 */
@Component
@RequiredArgsConstructor
class FlightRangeCallPlanner {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TravelpayoutsClient travelpayoutsClient;

    List<Callable<List<TravelpayoutsPriceItem>>> plan(FlightSearchRequestDto request) {
        List<String> destinations = FlightSearchDestinations.resolve(request);
        List<String> months = monthsOf(request.parsedDateFrom(), request.parsedDateTo());
        int destinationCount = Math.max(1, destinations.size());

        List<NightsWindow> nightsWindows = nightsWindowsFor(request, destinationCount, months.size());

        List<String> resolvedDestinations = FlightSearchDestinations.isSectorBased(request)
                ? FlightSearchDestinations.withAggregateIfBudgetAllows(
                        destinations, months.size() * nightsWindows.size())
                : destinations;

        return buildCalls(request.origin(), resolvedDestinations, months, nightsWindows);
    }

    private List<Callable<List<TravelpayoutsPriceItem>>> buildCalls(
            String origin, List<String> destinations, List<String> months, List<NightsWindow> nightsWindows) {
        List<String> callDestinations = FlightSearchDestinations.paddedForCalls(destinations);

        List<Callable<List<TravelpayoutsPriceItem>>> calls = new ArrayList<>();
        for (String destination : callDestinations) {
            for (String month : months) {
                for (NightsWindow window : nightsWindows) {
                    calls.add(() -> List.copyOf(travelpayoutsClient
                            .groupedPrices(origin, destination, month, window.min(), window.max())
                            .dataOrEmpty()
                            .values()));
                }
            }
        }
        return calls;
    }

    /**
     * nights를 명시적으로 안 줬으면 바로 범위 모드다(자동 확장은 최대 90개까지 갈 수 있어 개별로
     * 쪼개는 게 말이 안 된다). 명시적으로 줬으면 값 하나하나를 정확한 창으로 조회하는 개별 모드를
     * 우선 시도하고, 예산을 넘으면 범위 모드로 낮춘다.
     */
    private static List<NightsWindow> nightsWindowsFor(
            FlightSearchRequestDto request, int destinationCount, int monthCount) {
        if (CollectionUtils.isEmpty(request.nights())) {
            return List.of(rangeWindowOf(request.parsedNights()));
        }
        List<NightsWindow> exactWindows = exactWindowsOf(request.parsedNights());
        if (fitsWithinBudget(destinationCount, monthCount, exactWindows.size())) {
            return exactWindows;
        }
        return List.of(rangeWindowOf(request.parsedNights()));
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
    private static List<NightsWindow> exactWindowsOf(List<Integer> nights) {
        return nights.stream().distinct().map(n -> new NightsWindow(n, n)).toList();
    }

    /** nights 전체를 최솟값~최댓값 창 하나로 뭉갠다. */
    private static NightsWindow rangeWindowOf(List<Integer> nights) {
        return new NightsWindow(Collections.min(nights), Collections.max(nights));
    }

    private static boolean fitsWithinBudget(int destinationCount, int monthCount, int nightsWindowCount) {
        return (long) destinationCount * monthCount * nightsWindowCount <= FlightSearchDestinations.MAX_FANOUT_CALLS;
    }

    private record NightsWindow(int min, int max) {}
}
