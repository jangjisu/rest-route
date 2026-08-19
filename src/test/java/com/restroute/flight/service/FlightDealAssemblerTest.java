package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightDealAssemblerTest {

    private static final String VALID_ORIGIN = "ICN";
    private static final String VALID_DATE_FROM = LocalDate.now().plusDays(10).toString();
    private static final String VALID_DATE_TO = LocalDate.now().plusDays(41).toString();

    private final FlightRangeSearchResponseMapper responseMapper = mock(FlightRangeSearchResponseMapper.class);
    private final FlightDealPostFilter postFilter = mock(FlightDealPostFilter.class);
    private final FlightDealHolidayEnricher holidayEnricher = mock(FlightDealHolidayEnricher.class);
    private final FlightDealAssembler assembler = new FlightDealAssembler(responseMapper, postFilter, holidayEnricher);

    @Test
    @DisplayName("매핑 -> 필터 -> 공휴일 채우기 -> 최저가 표시 순서로 조립한다")
    void assemble_runsMapFilterEnrichThenMarksLowest() {
        FlightSearchRequestDto request = request();
        TravelpayoutsPriceItem rawItem = rawItem();
        FlightDealResponse mapped = dealWithPrice(89000);
        FlightDealResponse filtered = dealWithPrice(89000);
        FlightDealResponse enriched = dealWithPrice(89000);
        when(responseMapper.mapAll(List.of(rawItem), "TOK1")).thenReturn(List.of(mapped));
        when(postFilter.apply(List.of(mapped), request)).thenReturn(List.of(filtered));
        when(holidayEnricher.enrich(List.of(filtered))).thenReturn(List.of(enriched));

        List<FlightDealResponse> result = assembler.assemble(List.of(rawItem), "TOK1", request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLowestInRange()).isTrue();
    }

    @Test
    @DisplayName("필터를 거치며 항목이 빠지면, 남은 항목 중에서만 최저가를 표시한다")
    void assemble_marksLowestAmongSurvivingItemsOnly() {
        FlightSearchRequestDto request = request();
        TravelpayoutsPriceItem rawItem = rawItem();
        FlightDealResponse cheapButFiltered = dealWithPrice(50000);
        FlightDealResponse survivor = dealWithPrice(89000);
        when(responseMapper.mapAll(eq(List.of(rawItem)), eq("TOK1"))).thenReturn(List.of(cheapButFiltered, survivor));
        when(postFilter.apply(List.of(cheapButFiltered, survivor), request)).thenReturn(List.of(survivor));
        when(holidayEnricher.enrich(List.of(survivor))).thenReturn(List.of(survivor));

        List<FlightDealResponse> result = assembler.assemble(List.of(rawItem), "TOK1", request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).price().amount()).isEqualTo(89000);
        assertThat(result.get(0).isLowestInRange()).isTrue();
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

    private static FlightSearchRequestDto request() {
        return new FlightSearchRequestDto(
                VALID_ORIGIN,
                "range",
                VALID_DATE_FROM,
                VALID_DATE_TO,
                null,
                List.of("3"),
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
}
