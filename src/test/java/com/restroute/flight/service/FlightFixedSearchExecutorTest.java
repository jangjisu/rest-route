package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.exception.TravelpayoutsApiException;
import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightFixedSearchExecutorTest {

    @Mock
    private TravelpayoutsClient travelpayoutsClient;

    private FlightFixedSearchExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new FlightFixedSearchExecutor(travelpayoutsClient);
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

    @Test
    @DisplayName("destination을 지정하면 그 값 그대로 한 번 호출한다")
    void execute_callsOnceWithGivenDestination() {
        FlightSearchRequestDto request = request("OSA", null);
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "OSA", request.dateFrom(), request.dateTo()))
                .thenReturn(responseOf(item()));

        List<TravelpayoutsPriceItem> result = executor.execute(request);

        assertThat(result).hasSize(1);
        verify(travelpayoutsClient).groupedPricesForExactDates("ICN", "OSA", request.dateFrom(), request.dateTo());
    }

    @Test
    @DisplayName("sector를 지정하면 그 국가 개수만큼 호출하고, 전체 조회도 하나 더 얹어서 합친다")
    void execute_callsOncePerSectorCountry_plusAggregate() {
        FlightSearchRequestDto request = request(null, List.of("GUAM_SAIPAN"));
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "GU", request.dateFrom(), request.dateTo()))
                .thenReturn(responseOf(item()));
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "MP", request.dateFrom(), request.dateTo()))
                .thenReturn(responseOf());
        when(travelpayoutsClient.groupedPricesForExactDates(
                        eq("ICN"), isNull(), eq(request.dateFrom()), eq(request.dateTo())))
                .thenReturn(responseOf());

        List<TravelpayoutsPriceItem> result = executor.execute(request);

        assertThat(result).hasSize(1);
        verify(travelpayoutsClient).groupedPricesForExactDates("ICN", "GU", request.dateFrom(), request.dateTo());
        verify(travelpayoutsClient).groupedPricesForExactDates("ICN", "MP", request.dateFrom(), request.dateTo());
        verify(travelpayoutsClient)
                .groupedPricesForExactDates(eq("ICN"), isNull(), eq(request.dateFrom()), eq(request.dateTo()));
    }

    @Test
    @DisplayName("sector로 여러 국가 + 전체가 같은 항공권을 중복 반환하면 하나만 남긴다")
    void execute_dedupesWhenSectorAndAggregateReturnSameFlight() {
        FlightSearchRequestDto request = request(null, List.of("GUAM_SAIPAN"));
        TravelpayoutsPriceItem duplicate = item();
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "GU", request.dateFrom(), request.dateTo()))
                .thenReturn(responseOf(duplicate));
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "MP", request.dateFrom(), request.dateTo()))
                .thenReturn(responseOf());
        when(travelpayoutsClient.groupedPricesForExactDates(
                        eq("ICN"), isNull(), eq(request.dateFrom()), eq(request.dateTo())))
                .thenReturn(responseOf(duplicate));

        List<TravelpayoutsPriceItem> result = executor.execute(request);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("destination/sector 둘 다 없으면 destination을 null로 넘겨서 한 번만 호출한다")
    void execute_callsOnceWithNullDestination_whenNeitherGiven() {
        FlightSearchRequestDto request = request(null, null);
        when(travelpayoutsClient.groupedPricesForExactDates(
                        eq("ICN"), isNull(), eq(request.dateFrom()), eq(request.dateTo())))
                .thenReturn(responseOf(item()));

        List<TravelpayoutsPriceItem> result = executor.execute(request);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("호출 하나라도 실패하면 전체가 그대로 실패한다")
    void execute_propagatesFailure() {
        FlightSearchRequestDto request = request("OSA", null);
        when(travelpayoutsClient.groupedPricesForExactDates("ICN", "OSA", request.dateFrom(), request.dateTo()))
                .thenThrow(new TravelpayoutsApiException("grouped prices (exact dates)", "boom"));

        assertThatThrownBy(() -> executor.execute(request)).isInstanceOf(TravelpayoutsApiException.class);
    }
}
