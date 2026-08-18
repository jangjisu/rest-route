package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.response.FlightDealResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightRangeSearchResponseMapperTest {

    @Mock
    private FlightAirportNameCache airportNameCache;

    @Mock
    private FlightAirlineNameCache airlineNameCache;

    private FlightRangeSearchResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FlightRangeSearchResponseMapper(airportNameCache, airlineNameCache);
    }

    private static TravelpayoutsPriceItem item() {
        return new TravelpayoutsPriceItem(
                "SEL",
                "OSA",
                "ICN",
                "KIX",
                89000,
                "LJ",
                "123",
                "2026-09-15T09:20:00+09:00",
                "2026-09-18T13:10:00+09:00",
                1,
                2,
                999,
                90,
                90,
                "Aviasales",
                "https://example.com/link");
    }

    @Test
    @DisplayName("목적지는 공항코드 기준으로 이름을 채운다")
    void mapAll_fillsDestinationByAirportCode() {
        when(airportNameCache.findName("KIX")).thenReturn("오사카");

        List<FlightDealResponse> result = mapper.mapAll(List.of(item()), "TOK1");

        assertThat(result.get(0).destination()).isEqualTo(new FlightDealResponse.Destination("KIX", "오사카"));
    }

    @Test
    @DisplayName("항공사 이름을 채우고 isLowCost는 항상 false다")
    void mapAll_fillsAirlineName_andIsLowCostAlwaysFalse() {
        when(airlineNameCache.findName("LJ")).thenReturn("진에어");

        List<FlightDealResponse> result = mapper.mapAll(List.of(item()), "TOK1");

        assertThat(result.get(0).airline()).isEqualTo(new FlightDealResponse.Airline("LJ", "진에어", false));
    }

    @Test
    @DisplayName("출/도착 시각과 소요시간, 경유 횟수를 각 leg에 매핑한다")
    void mapAll_mapsLegsFromDepartureAndReturn() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(item()), "TOK1");
        FlightDealResponse deal = result.get(0);

        assertThat(deal.departure().departAt()).isEqualTo("2026-09-15T09:20:00+09:00");
        assertThat(deal.departure().arriveAt()).isEqualTo("2026-09-15T10:50:00+09:00");
        assertThat(deal.departure().duration()).isEqualTo(90);
        assertThat(deal.departure().transferCount()).isEqualTo(1);

        assertThat(deal.arrival().departAt()).isEqualTo("2026-09-18T13:10:00+09:00");
        assertThat(deal.arrival().arriveAt()).isEqualTo("2026-09-18T14:40:00+09:00");
        assertThat(deal.arrival().duration()).isEqualTo(90);
        assertThat(deal.arrival().transferCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("nights는 출발일과 귀국일의 날짜 차이로 계산한다")
    void mapAll_computesNightsFromDateDifference() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(item()), "TOK1");

        assertThat(result.get(0).nights()).isEqualTo(3);
    }

    @Test
    @DisplayName("price/gate/link/seatsLeft/holiday는 그대로 옮기거나 스텁으로 채운다")
    void mapAll_fillsRemainingFieldsAndStubs() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(item()), "TOK1");
        FlightDealResponse deal = result.get(0);

        assertThat(deal.price()).isEqualTo(new FlightDealResponse.Price(89000, "KRW"));
        assertThat(deal.gateName()).isEqualTo("Aviasales");
        assertThat(deal.bookingLink()).isEqualTo("https://example.com/link");
        assertThat(deal.seatsLeft()).isNull();
        assertThat(deal.holiday()).isEqualTo(new FlightDealResponse.Holiday(0, List.of(), 0));
    }

    @Test
    @DisplayName("id는 세션 토큰과 순번(1부터, 4자리)으로 매긴다")
    void mapAll_assignsIdFromSessionTokenAndIndex() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(item(), item()), "TOK1");

        assertThat(result).extracting(FlightDealResponse::id).containsExactly("TOK1_0001", "TOK1_0002");
    }

    @Test
    @DisplayName("가격 기준 전체 최저가 한 건에만 isLowestInRange가 true다")
    void mapAll_marksOnlyOverallLowestPrice() {
        TravelpayoutsPriceItem cheaper = new TravelpayoutsPriceItem(
                "SEL",
                "OSA",
                "ICN",
                "KIX",
                50000,
                "LJ",
                "999",
                "2026-09-20T09:20:00+09:00",
                "2026-09-23T13:10:00+09:00",
                0,
                0,
                999,
                90,
                90,
                "Aviasales",
                "https://example.com/cheaper");

        List<FlightDealResponse> result = mapper.mapAll(List.of(item(), cheaper), "TOK1");

        assertThat(result.get(0).isLowestInRange()).isFalse();
        assertThat(result.get(1).isLowestInRange()).isTrue();
    }
}
