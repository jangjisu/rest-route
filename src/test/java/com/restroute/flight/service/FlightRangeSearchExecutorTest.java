package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.exception.TravelpayoutsApiException;
import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightRangeSearchExecutorTest {

    @Mock
    private TravelpayoutsClient travelpayoutsClient;

    private FlightRangeSearchExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new FlightRangeSearchExecutor(travelpayoutsClient);
    }

    private static TravelpayoutsPriceItem itemAt(String departureAt) {
        return new TravelpayoutsPriceItem(
                "SEL", "OSA", "ICN", "KIX", 100000, "LJ", "123", departureAt, null, 0, 0, 90, 90, 0, "gate", "link");
    }

    private static TravelpayoutsGroupedPricesResponse responseOf(TravelpayoutsPriceItem... items) {
        Map<String, TravelpayoutsPriceItem> data = new java.util.LinkedHashMap<>();
        for (TravelpayoutsPriceItem item : items) {
            data.put(item.departureAt().substring(0, 10), item);
        }
        return new TravelpayoutsGroupedPricesResponse(true, "krw", data);
    }

    @Test
    @DisplayName("destination이 있으면 그 값 그대로 넘겨서 호출한다")
    void execute_callsWithGivenDestination() {
        FlightRangeSearchPlan plan = new FlightRangeSearchPlan(
                List.of("JP"), List.of("2026-09"), List.of(new FlightRangeSearchPlan.NightsWindow(3, 5)));
        when(travelpayoutsClient.groupedPrices("ICN", "JP", "2026-09", 3, 5))
                .thenReturn(responseOf(itemAt("2026-09-15T09:00:00+09:00")));

        List<TravelpayoutsPriceItem> result =
                executor.execute("ICN", plan, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(result).hasSize(1);
        verify(travelpayoutsClient).groupedPrices("ICN", "JP", "2026-09", 3, 5);
    }

    @Test
    @DisplayName("destinations가 비어있으면 destination을 null로 넘겨서 호출한다(생략)")
    void execute_passesNullDestination_whenOmitted() {
        FlightRangeSearchPlan plan = new FlightRangeSearchPlan(
                List.of(), List.of("2026-09"), List.of(new FlightRangeSearchPlan.NightsWindow(3, 5)));
        when(travelpayoutsClient.groupedPrices(eq("ICN"), isNull(), eq("2026-09"), eq(3), eq(5)))
                .thenReturn(responseOf());

        executor.execute("ICN", plan, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        verify(travelpayoutsClient).groupedPrices(eq("ICN"), isNull(), eq("2026-09"), eq(3), eq(5));
    }

    @Test
    @DisplayName("destinations x months x nightsWindows 조합 수만큼 정확히 호출한다")
    void execute_callsOnceForEachCombination() {
        FlightRangeSearchPlan plan = new FlightRangeSearchPlan(
                List.of("JP", "TW"),
                List.of("2026-09", "2026-10"),
                List.of(new FlightRangeSearchPlan.NightsWindow(3, 3), new FlightRangeSearchPlan.NightsWindow(4, 4)));
        when(travelpayoutsClient.groupedPrices(any(), any(), any(), any(), any()))
                .thenReturn(responseOf());

        executor.execute("ICN", plan, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));

        verify(travelpayoutsClient, times(8)).groupedPrices(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("여러 호출의 결과를 하나로 합친다")
    void execute_mergesResultsFromMultipleCalls() {
        FlightRangeSearchPlan plan = new FlightRangeSearchPlan(
                List.of("JP", "TW"), List.of("2026-09"), List.of(new FlightRangeSearchPlan.NightsWindow(3, 5)));
        when(travelpayoutsClient.groupedPrices("ICN", "JP", "2026-09", 3, 5))
                .thenReturn(responseOf(itemAt("2026-09-10T09:00:00+09:00")));
        when(travelpayoutsClient.groupedPrices("ICN", "TW", "2026-09", 3, 5))
                .thenReturn(responseOf(itemAt("2026-09-20T09:00:00+09:00")));

        List<TravelpayoutsPriceItem> result =
                executor.execute("ICN", plan, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("달 전체를 받아온 결과 중 실제 요청 범위(dateFrom~dateTo) 밖은 걸러낸다")
    void execute_filtersOutItemsOutsideRequestedDateRange() {
        FlightRangeSearchPlan plan = new FlightRangeSearchPlan(
                List.of("JP"), List.of("2026-09"), List.of(new FlightRangeSearchPlan.NightsWindow(3, 5)));
        when(travelpayoutsClient.groupedPrices("ICN", "JP", "2026-09", 3, 5))
                .thenReturn(responseOf(
                        itemAt("2026-09-05T09:00:00+09:00"),
                        itemAt("2026-09-15T09:00:00+09:00"),
                        itemAt("2026-09-25T09:00:00+09:00")));

        List<TravelpayoutsPriceItem> result =
                executor.execute("ICN", plan, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20));

        assertThat(result)
                .extracting(item -> item.departureAt().substring(0, 10))
                .containsExactly("2026-09-15");
    }

    @Test
    @DisplayName("국가별 조회와 전체 조회가 같은 항공권을 중복 반환하면 하나만 남긴다")
    void execute_dedupesWhenCountryAndAggregateReturnSameFlight() {
        FlightRangeSearchPlan plan = new FlightRangeSearchPlan(
                Arrays.asList("JP", null), List.of("2026-09"), List.of(new FlightRangeSearchPlan.NightsWindow(3, 5)));
        TravelpayoutsPriceItem sameFlight = itemAt("2026-09-15T09:00:00+09:00");
        when(travelpayoutsClient.groupedPrices("ICN", "JP", "2026-09", 3, 5)).thenReturn(responseOf(sameFlight));
        when(travelpayoutsClient.groupedPrices(eq("ICN"), isNull(), eq("2026-09"), eq(3), eq(5)))
                .thenReturn(responseOf(sameFlight));

        List<TravelpayoutsPriceItem> result =
                executor.execute("ICN", plan, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("호출 하나라도 실패하면 전체 검색이 그대로 실패한다")
    void execute_propagatesFailureFromAnyCall() {
        FlightRangeSearchPlan plan = new FlightRangeSearchPlan(
                List.of("JP"), List.of("2026-09"), List.of(new FlightRangeSearchPlan.NightsWindow(3, 5)));
        when(travelpayoutsClient.groupedPrices("ICN", "JP", "2026-09", 3, 5))
                .thenThrow(new TravelpayoutsApiException("grouped prices", "boom"));

        assertThatThrownBy(() -> executor.execute("ICN", plan, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .isInstanceOf(TravelpayoutsApiException.class);
    }
}
