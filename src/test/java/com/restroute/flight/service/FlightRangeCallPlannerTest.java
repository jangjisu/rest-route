package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightRangeCallPlannerTest {

    @Mock
    private TravelpayoutsClient travelpayoutsClient;

    private FlightRangeCallPlanner planner;

    @BeforeEach
    void setUp() {
        planner = new FlightRangeCallPlanner(travelpayoutsClient);
    }

    /** 오늘이 며칠이든 항상 같은 달 안에 머무는 날짜 — 개월 축이 1로 고정되는 테스트에 쓴다. */
    private static LocalDate singleMonthDateFrom() {
        return LocalDate.now().plusMonths(1).withDayOfMonth(1);
    }

    private static FlightSearchRequestDto request(
            String dateFrom, String dateTo, String destination, List<String> sector, List<String> nights) {
        return new FlightSearchRequestDto(
                "ICN",
                "range",
                dateFrom,
                dateTo,
                destination,
                nights,
                sector,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static TravelpayoutsGroupedPricesResponse responseOf(TravelpayoutsPriceItem... items) {
        Map<String, TravelpayoutsPriceItem> data = new java.util.LinkedHashMap<>();
        for (int i = 0; i < items.length; i++) {
            data.put("k" + i, items[i]);
        }
        return new TravelpayoutsGroupedPricesResponse(true, "krw", data);
    }

    private static TravelpayoutsPriceItem item() {
        return new TravelpayoutsPriceItem(
                "SEL",
                "OSA",
                "ICN",
                "KIX",
                100000,
                "LJ",
                "123",
                "2026-09-15T09:00:00+09:00",
                "2026-09-18T09:00:00+09:00",
                0,
                0,
                90,
                90,
                0,
                "gate",
                "link");
    }

    private static List<TravelpayoutsPriceItem> runAll(List<Callable<List<TravelpayoutsPriceItem>>> calls)
            throws Exception {
        List<TravelpayoutsPriceItem> merged = new java.util.ArrayList<>();
        for (Callable<List<TravelpayoutsPriceItem>> call : calls) {
            merged.addAll(call.call());
        }
        return merged;
    }

    @Test
    @DisplayName("destination을 직접 지정하면 그 값 그대로 한 번만 호출하고, 예산이 남아도 전체 조회를 얹지 않는다")
    void plan_callsOnceWithGivenDestination_neverAddsAggregate() throws Exception {
        LocalDate dateFrom = singleMonthDateFrom();
        FlightSearchRequestDto request =
                request(dateFrom.toString(), dateFrom.plusDays(10).toString(), "OSA", null, List.of("3", "4", "5"));
        when(travelpayoutsClient.groupedPrices(eq("ICN"), eq("OSA"), any(), any(), any()))
                .thenReturn(responseOf());

        List<Callable<List<TravelpayoutsPriceItem>>> calls = planner.plan(request);

        assertThat(calls).hasSize(3);
        runAll(calls);
        verify(travelpayoutsClient, times(3)).groupedPrices(eq("ICN"), eq("OSA"), any(), any(), any());
    }

    @Test
    @DisplayName("sector/destination이 둘 다 없으면 destination을 null로 넘겨서 한 번만 호출한다")
    void plan_callsOnceWithNullDestination_whenNeitherGiven() throws Exception {
        LocalDate dateFrom = singleMonthDateFrom();
        FlightSearchRequestDto request =
                request(dateFrom.toString(), dateFrom.plusDays(10).toString(), null, null, List.of("3"));
        when(travelpayoutsClient.groupedPrices(eq("ICN"), isNull(), any(), any(), any()))
                .thenReturn(responseOf(item()));

        List<Callable<List<TravelpayoutsPriceItem>>> calls = planner.plan(request);
        List<TravelpayoutsPriceItem> result = runAll(calls);

        assertThat(result).hasSize(1);
        verify(travelpayoutsClient).groupedPrices(eq("ICN"), isNull(), any(), any(), any());
    }

    @Test
    @DisplayName("예산(20) 안이면 nights를 요청한 값 각각 정확한 창으로 쪼개 호출한다")
    void plan_usesExactNightsWindows_whenWithinBudget() throws Exception {
        LocalDate dateFrom = singleMonthDateFrom();
        FlightSearchRequestDto request = request(
                dateFrom.toString(), dateFrom.plusDays(10).toString(), null, List.of("JAPAN"), List.of("3", "4", "5"));
        when(travelpayoutsClient.groupedPrices(any(), any(), any(), any(), any()))
                .thenReturn(responseOf());

        List<Callable<List<TravelpayoutsPriceItem>>> calls = planner.plan(request);
        runAll(calls);

        // JAPAN(1개국) + 전체 = 2 destinations x 1개월 x nights 3개(3,4,5 개별) = 6
        verify(travelpayoutsClient).groupedPrices("ICN", "JP", monthOf(request), 3, 3);
        verify(travelpayoutsClient).groupedPrices("ICN", "JP", monthOf(request), 4, 4);
        verify(travelpayoutsClient).groupedPrices("ICN", "JP", monthOf(request), 5, 5);
        verify(travelpayoutsClient, times(6)).groupedPrices(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("sector를 4개 다 고르면(9개국) nights 3개와 곱해 20을 넘어서 범위 모드로 낮추고, 전체 조회를 하나 더 얹는다")
    void plan_downgradesToRangeWindow_whenExactNightsExceedsBudget() throws Exception {
        LocalDate dateFrom = singleMonthDateFrom();
        FlightSearchRequestDto request = request(
                dateFrom.toString(),
                dateFrom.plusDays(10).toString(),
                null,
                List.of("JAPAN", "SOUTHEAST_ASIA", "GREATER_CHINA", "GUAM_SAIPAN"),
                List.of("3", "4", "5"));
        when(travelpayoutsClient.groupedPrices(any(), any(), any(), any(), any()))
                .thenReturn(responseOf());

        List<Callable<List<TravelpayoutsPriceItem>>> calls = planner.plan(request);
        runAll(calls);

        // 9개국 + 전체 = 10 destinations x 1개월 x 범위창 1개 = 10 (범위 모드라 min=3,max=5 창 하나)
        verify(travelpayoutsClient, times(10)).groupedPrices(any(), any(), any(), eq(3), eq(5));
    }

    @Test
    @DisplayName("국가별+전체를 합쳐도 예산을 넘으면 국가별 조회를 포기하고 전체 조회 하나만 한다")
    void plan_dropsPerCountryDestinations_whenAggregateBudgetExceeded() throws Exception {
        LocalDate dateFrom = LocalDate.now();
        LocalDate dateTo = dateFrom.plusMonths(3);
        FlightSearchRequestDto request = request(
                dateFrom.toString(),
                dateTo.toString(),
                null,
                List.of("JAPAN", "SOUTHEAST_ASIA", "GREATER_CHINA", "GUAM_SAIPAN"),
                List.of("3"));
        when(travelpayoutsClient.groupedPrices(any(), any(), any(), any(), any()))
                .thenReturn(responseOf());

        List<Callable<List<TravelpayoutsPriceItem>>> calls = planner.plan(request);
        runAll(calls);

        // 4개월 x 전체(destination=null) 하나만 = 4
        verify(travelpayoutsClient, times(4)).groupedPrices(eq("ICN"), isNull(), any(), any(), any());
        verify(travelpayoutsClient, times(4)).groupedPrices(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("여러 호출의 결과를 하나로 합친다")
    void plan_mergesResultsFromMultipleCalls() throws Exception {
        LocalDate dateFrom = singleMonthDateFrom();
        FlightSearchRequestDto request =
                request(dateFrom.toString(), dateFrom.plusDays(10).toString(), null, List.of("JAPAN"), List.of("3"));
        when(travelpayoutsClient.groupedPrices(eq("ICN"), eq("JP"), any(), any(), any()))
                .thenReturn(responseOf(item()));
        when(travelpayoutsClient.groupedPrices(eq("ICN"), isNull(), any(), any(), any()))
                .thenReturn(responseOf(item()));

        List<TravelpayoutsPriceItem> result = runAll(planner.plan(request));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("dateFrom~dateTo가 두 달에 걸치면 두 달 각각 호출한다")
    void plan_callsOnceForEachCalendarMonthTouched() throws Exception {
        LocalDate dateFrom =
                java.time.YearMonth.from(LocalDate.now().plusDays(20)).atEndOfMonth();
        LocalDate dateTo = dateFrom.plusDays(1);
        FlightSearchRequestDto request = request(dateFrom.toString(), dateTo.toString(), "OSA", null, List.of("3"));
        when(travelpayoutsClient.groupedPrices(any(), any(), any(), any(), any()))
                .thenReturn(responseOf());

        runAll(planner.plan(request));

        verify(travelpayoutsClient).groupedPrices(eq("ICN"), eq("OSA"), eq(yearMonthOf(dateFrom)), eq(3), eq(3));
        verify(travelpayoutsClient).groupedPrices(eq("ICN"), eq("OSA"), eq(yearMonthOf(dateTo)), eq(3), eq(3));
    }

    private static String monthOf(FlightSearchRequestDto request) {
        return yearMonthOf(request.parsedDateFrom());
    }

    private static String yearMonthOf(LocalDate date) {
        return date.getYear() + "-" + "%02d".formatted(date.getMonthValue());
    }
}
