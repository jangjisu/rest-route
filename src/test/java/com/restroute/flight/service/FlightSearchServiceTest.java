package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("첫 요청(cursor 없음)이면 dealFetcher로 조회해 세션에 저장하고 결과를 반환한다")
    void search_fetchesFromDealFetcher_whenFirstRequest() {
        FlightDealFetcher dealFetcher = mock(FlightDealFetcher.class);
        FlightSearchService service = FlightSearchService.create(new FlightDealSessionStore(), dealFetcher);
        FlightSearchRequestDto request = request(null, "3", null);
        FlightDealResponse mapped = dealWithPrice(89000);
        when(dealFetcher.fetch(request)).thenReturn(List.of(mapped));

        FlightDealSearchResponse response = service.search(request);

        assertThat(response.items())
                .extracting(FlightDealResponse::price)
                .containsExactly(new FlightDealResponse.Price(89000, "KRW"));
        verify(dealFetcher).fetch(request);
    }

    @Test
    @DisplayName("cursor가 이전 세션을 이어가면 dealFetcher를 다시 부르지 않고 이어서 반환한다")
    void search_doesNotRefetch_whenCursorContinuesExistingSession() {
        FlightDealFetcher dealFetcher = mock(FlightDealFetcher.class);
        FlightSearchService service = FlightSearchService.create(new FlightDealSessionStore(), dealFetcher);
        FlightSearchRequestDto firstRequest = request(null, "3", "1");
        when(dealFetcher.fetch(firstRequest)).thenReturn(List.of(dealWithPrice(89000), dealWithPrice(95000)));
        FlightDealSearchResponse first = service.search(firstRequest);

        FlightSearchRequestDto secondRequest = request(first.meta().nextCursor(), "3", "1");
        FlightDealSearchResponse second = service.search(secondRequest);

        assertThat(second.items())
                .extracting(FlightDealResponse::price)
                .containsExactly(new FlightDealResponse.Price(95000, "KRW"));
        verify(dealFetcher, times(1)).fetch(any());
    }

    private static FlightDealResponse dealWithPrice(int amount) {
        FlightDealResponse.Leg leg =
                new FlightDealResponse.Leg("2026-09-15T09:00:00+09:00", "2026-09-15T10:30:00+09:00", 90, 0);
        return new FlightDealResponse(
                "",
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
                cursor,
                limit,
                null,
                null);
    }
}
