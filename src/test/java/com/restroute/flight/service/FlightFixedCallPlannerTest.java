package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
class FlightFixedCallPlannerTest {

    @Mock
    private TravelpayoutsClient travelpayoutsClient;

    private FlightFixedCallPlanner planner;

    @BeforeEach
    void setUp() {
        planner = new FlightFixedCallPlanner(travelpayoutsClient);
    }

    private static String futureDate(int daysFromToday) {
        return LocalDate.now().plusDays(daysFromToday).toString();
    }

    private static FlightSearchRequestDto request(String destination, List<String> sector) {
        return new FlightSearchRequestDto(
                "ICN",
                "fixed",
                futureDate(10),
                futureDate(13),
                destination,
                null,
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

    private static TravelpayoutsPriceItem item() {
        return new TravelpayoutsPriceItem(
                "SEL", "OSA", "ICN", "KIX", 89000, "LJ", "1", "2026-x", "2026-y", 0, 0, 90, 90, 0, "gate", "link");
    }

    private static TravelpayoutsGroupedPricesResponse responseOf(TravelpayoutsPriceItem... items) {
        Map<String, TravelpayoutsPriceItem> data = new java.util.LinkedHashMap<>();
        for (int i = 0; i < items.length; i++) {
            data.put("k" + i, items[i]);
        }
        return new TravelpayoutsGroupedPricesResponse(true, "krw", data);
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
    @DisplayName("destination을 지정하면 그 값 그대로 한 번 호출하는 Callable 하나를 만든다")
    void plan_buildsOneCallable_forGivenDestination() throws Exception {
        FlightSearchRequestDto request = request("OSA", null);
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "OSA", request.dateFrom(), request.dateTo()))
                .thenReturn(responseOf(item()));

        List<Callable<List<TravelpayoutsPriceItem>>> calls = planner.plan(request);

        assertThat(calls).hasSize(1);
        assertThat(runAll(calls)).hasSize(1);
        verify(travelpayoutsClient).groupedPricesForExactDates("ICN", "OSA", request.dateFrom(), request.dateTo());
    }

    @Test
    @DisplayName("sector를 지정하면 그 국가 개수만큼 + 전체 조회 Callable을 만든다")
    void plan_buildsOneCallablePerSectorCountry_plusAggregate() throws Exception {
        FlightSearchRequestDto request = request(null, List.of("GUAM_SAIPAN"));
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "GU", request.dateFrom(), request.dateTo()))
                .thenReturn(responseOf());
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "MP", request.dateFrom(), request.dateTo()))
                .thenReturn(responseOf());
        when(travelpayoutsClient.groupedPricesForExactDates(
                        eq("ICN"), isNull(), eq(request.dateFrom()), eq(request.dateTo())))
                .thenReturn(responseOf());

        List<Callable<List<TravelpayoutsPriceItem>>> calls = planner.plan(request);
        runAll(calls);

        assertThat(calls).hasSize(3);
        verify(travelpayoutsClient).groupedPricesForExactDates("ICN", "GU", request.dateFrom(), request.dateTo());
        verify(travelpayoutsClient).groupedPricesForExactDates("ICN", "MP", request.dateFrom(), request.dateTo());
        verify(travelpayoutsClient)
                .groupedPricesForExactDates(eq("ICN"), isNull(), eq(request.dateFrom()), eq(request.dateTo()));
    }

    @Test
    @DisplayName("destination/sector 둘 다 없으면 destination을 null로 넘기는 Callable 하나만 만든다")
    void plan_buildsOneCallableWithNullDestination_whenNeitherGiven() throws Exception {
        FlightSearchRequestDto request = request(null, null);
        when(travelpayoutsClient.groupedPricesForExactDates(
                        eq("ICN"), isNull(), eq(request.dateFrom()), eq(request.dateTo())))
                .thenReturn(responseOf(item()));

        List<Callable<List<TravelpayoutsPriceItem>>> calls = planner.plan(request);

        assertThat(calls).hasSize(1);
        assertThat(runAll(calls)).hasSize(1);
    }
}
