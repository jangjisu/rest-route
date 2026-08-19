package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchServiceTest {

    private static final String VALID_ORIGIN = "ICN";
    private static final String VALID_DATE_FROM = LocalDate.now().plusDays(10).toString();
    private static final String VALID_DATE_TO = LocalDate.now().plusDays(41).toString();

    @Test
    @DisplayName("RANGE는 rangeExecutor를 통해 매핑/필터/공휴일/최저가 표시까지 전부 거친다")
    void search_usesRangeExecutorAndFullPipeline_whenSearchModeIsRange() {
        FlightRangeSearchExecutor rangeExecutor = mock(FlightRangeSearchExecutor.class);
        FlightFixedSearchExecutor fixedExecutor = mock(FlightFixedSearchExecutor.class);
        FlightRangeSearchResponseMapper responseMapper = mock(FlightRangeSearchResponseMapper.class);
        FlightDealPostFilter postFilter = mock(FlightDealPostFilter.class);
        FlightDealHolidayEnricher holidayEnricher = mock(FlightDealHolidayEnricher.class);
        FlightSearchService service = new FlightSearchService(
                new FlightDealSessionStore(),
                rangeExecutor,
                fixedExecutor,
                responseMapper,
                postFilter,
                holidayEnricher);
        FlightSearchRequestDto request = request(null, "3", null);

        TravelpayoutsPriceItem rawItem = rawItem();
        when(rangeExecutor.execute(eq(VALID_ORIGIN), any(), eq(request.parsedDateFrom()), eq(request.parsedDateTo())))
                .thenReturn(List.of(rawItem));
        FlightDealResponse mapped = dealWithPrice(89000);
        when(responseMapper.mapAll(eq(List.of(rawItem)), anyString())).thenReturn(List.of(mapped));
        when(postFilter.apply(anyList(), eq(request))).thenReturn(List.of(mapped));
        when(holidayEnricher.enrich(List.of(mapped))).thenReturn(List.of(mapped));

        FlightDealSearchResponse response = service.search(request);

        assertThat(response.items())
                .extracting(FlightDealResponse::price)
                .containsExactly(new FlightDealResponse.Price(89000, "KRW"));
        assertThat(response.items().get(0).isLowestInRange()).isTrue();
        verify(fixedExecutor, never()).execute(any());
    }

    @Test
    @DisplayName("FIXED는 fixedExecutor를 통해 조회한다")
    void search_usesFixedExecutor_whenSearchModeIsFixed() {
        FlightRangeSearchExecutor rangeExecutor = mock(FlightRangeSearchExecutor.class);
        FlightFixedSearchExecutor fixedExecutor = mock(FlightFixedSearchExecutor.class);
        FlightRangeSearchResponseMapper responseMapper = mock(FlightRangeSearchResponseMapper.class);
        FlightDealPostFilter postFilter = mock(FlightDealPostFilter.class);
        FlightDealHolidayEnricher holidayEnricher = mock(FlightDealHolidayEnricher.class);
        FlightSearchService service = new FlightSearchService(
                new FlightDealSessionStore(),
                rangeExecutor,
                fixedExecutor,
                responseMapper,
                postFilter,
                holidayEnricher);
        FlightSearchRequestDto request = fixedRequest();

        when(fixedExecutor.execute(request)).thenReturn(List.of());
        when(responseMapper.mapAll(anyList(), anyString())).thenReturn(List.of());
        when(postFilter.apply(anyList(), eq(request))).thenReturn(List.of());
        when(holidayEnricher.enrich(List.of())).thenReturn(List.of());

        service.search(request);

        verify(fixedExecutor).execute(request);
        verify(rangeExecutor, never()).execute(any(), any(), any(), any());
    }

    private static TravelpayoutsPriceItem rawItem() {
        return new TravelpayoutsPriceItem(
                "SEL",
                "OSA",
                "ICN",
                "KIX",
                89000,
                "LJ",
                "1",
                "2026-09-15T09:00:00+09:00",
                "2026-09-18T09:00:00+09:00",
                0,
                0,
                90,
                90,
                90,
                "gate",
                "link");
    }

    private static FlightDealResponse dealWithPrice(int amount) {
        FlightDealResponse.Leg leg =
                new FlightDealResponse.Leg("2026-09-15T09:00:00+09:00", "2026-09-15T10:30:00+09:00", 90, 0);
        return new FlightDealResponse(
                "T_0001",
                new FlightDealResponse.Destination("KIX", "오사카"),
                leg,
                leg,
                3,
                List.of(),
                new FlightDealResponse.Airline("LJ", "진에어", false),
                new FlightDealResponse.Price(amount, "KRW"),
                false,
                "gate",
                "link",
                null);
    }

    private static FlightSearchRequestDto fixedRequest() {
        return new FlightSearchRequestDto(
                VALID_ORIGIN,
                "fixed",
                VALID_DATE_FROM,
                LocalDate.now().plusDays(13).toString(),
                "OSA",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static FlightSearchRequestDto request(String cursor, String nights, String limit) {
        return new FlightSearchRequestDto(
                VALID_ORIGIN,
                "range",
                VALID_DATE_FROM,
                VALID_DATE_TO,
                null,
                List.of(nights),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                cursor,
                limit,
                null,
                null);
    }
}
