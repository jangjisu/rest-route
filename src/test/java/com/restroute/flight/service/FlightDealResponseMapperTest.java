package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.domain.FlightAirlineEntity;
import com.restroute.flight.domain.FlightAirportEntity;
import com.restroute.flight.repository.FlightAirlineRepository;
import com.restroute.flight.repository.FlightAirportRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightDealResponseMapperTest {

    @Mock
    private FlightAirportRepository airportRepository;

    @Mock
    private FlightAirlineRepository airlineRepository;

    private FlightDealResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FlightDealResponseMapper(airportRepository, airlineRepository);
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

    private static TravelpayoutsPriceItem itemWithoutReturnDate() {
        return new TravelpayoutsPriceItem(
                "SEL",
                "OSA",
                "ICN",
                "KIX",
                89000,
                "LJ",
                "123",
                "2026-09-15T09:20:00+09:00",
                null,
                0,
                0,
                90,
                90,
                90,
                "gate",
                "link");
    }

    @Test
    @DisplayName("목적지는 공항코드 기준으로 이름을 채운다")
    void mapAll_fillsDestinationByAirportCode() {
        when(airportRepository.findByCode("KIX"))
                .thenReturn(Optional.of(new FlightAirportEntity("KIX", "오사카", "Osaka", "OSA", "JP")));

        List<FlightDealResponse> result = mapper.mapAll(List.of(item()));

        assertThat(result.get(0).destination()).isEqualTo(new FlightDealResponse.Destination("KIX", "오사카"));
    }

    @Test
    @DisplayName("목적지 공항을 DB에서 찾지 못하면 이름은 null로 채운다")
    void mapAll_leavesDestinationNameNullWhenAirportNotFound() {
        when(airportRepository.findByCode("KIX")).thenReturn(Optional.empty());

        List<FlightDealResponse> result = mapper.mapAll(List.of(item()));

        assertThat(result.get(0).destination()).isEqualTo(new FlightDealResponse.Destination("KIX", null));
    }

    @Test
    @DisplayName("항공사 이름과 저비용 여부를 DB에서 채운다")
    void mapAll_fillsAirlineNameAndLowCostFromRepository() {
        when(airlineRepository.findByCode("LJ"))
                .thenReturn(Optional.of(FlightAirlineEntity.of("LJ", "진에어", "Jin Air", true)));

        List<FlightDealResponse> result = mapper.mapAll(List.of(item()));

        assertThat(result.get(0).airline()).isEqualTo(new FlightDealResponse.Airline("LJ", "진에어", true));
    }

    @Test
    @DisplayName("항공사를 DB에서 찾지 못하면 이름은 null, 저비용 여부는 false로 채운다")
    void mapAll_fillsAirlineDefaultsWhenNotFound() {
        when(airlineRepository.findByCode("LJ")).thenReturn(Optional.empty());

        List<FlightDealResponse> result = mapper.mapAll(List.of(item()));

        assertThat(result.get(0).airline()).isEqualTo(new FlightDealResponse.Airline("LJ", null, false));
    }

    @Test
    @DisplayName("출/도착 시각과 소요시간, 경유 횟수를 각 leg에 매핑한다")
    void mapAll_mapsLegsFromDepartureAndReturn() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(item()));
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
        List<FlightDealResponse> result = mapper.mapAll(List.of(item()));

        assertThat(result.get(0).nights()).isEqualTo(3);
    }

    @Test
    @DisplayName("price/gate/link/seatsLeft/holidays는 그대로 옮기거나 스텁으로 채운다")
    void mapAll_fillsRemainingFieldsAndStubs() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(item()));
        FlightDealResponse deal = result.get(0);

        assertThat(deal.price()).isEqualTo(new FlightDealResponse.Price(89000, "KRW"));
        assertThat(deal.gateName()).isEqualTo("Aviasales");
        assertThat(deal.bookingLink()).isEqualTo("https://example.com/link");
        assertThat(deal.seatsLeft()).isNull();
        assertThat(deal.holidays()).isEmpty();
    }

    @Test
    @DisplayName("id는 세션 토큰을 아직 몰라서 채우지 않는다 — 세션 스토어가 나중에 부여한다")
    void mapAll_leavesIdUnassigned() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(item(), item()));

        assertThat(result).extracting(FlightDealResponse::id).containsExactly("", "");
    }

    @Test
    @DisplayName("isLowestInRange는 여기서 표시하지 않는다 — 필터를 다 거친 다음 별도로 표시해야 한다")
    void mapAll_neverMarksLowestInRange() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(item(), item()));

        assertThat(result).extracting(FlightDealResponse::isLowestInRange).containsExactly(false, false);
    }

    @Test
    @DisplayName("return_at이 없는 항목은 조용히 뺀다")
    void mapAll_dropsItemsMissingReturnDate() {
        List<FlightDealResponse> result = mapper.mapAll(List.of(itemWithoutReturnDate(), item()));

        assertThat(result).hasSize(1);
    }
}
