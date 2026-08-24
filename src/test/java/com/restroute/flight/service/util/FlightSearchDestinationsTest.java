package com.restroute.flight.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchDestinationsTest {

    private static String futureDate(int daysFromToday) {
        return LocalDate.now().plusDays(daysFromToday).toString();
    }

    @Test
    @DisplayName("destination을 직접 지정하면 그거 하나뿐이다")
    void resolve_returnsSingleDestination_whenGiven() {
        FlightSearchRequestDto request = request("OSA", null);

        assertThat(FlightSearchDestinations.resolve(request)).containsExactly("OSA");
    }

    @Test
    @DisplayName("sector를 지정하면 그 sector들의 국가 목록이다")
    void resolve_returnsSectorCountries_whenSectorGiven() {
        FlightSearchRequestDto request = request(null, List.of("JAPAN"));

        assertThat(FlightSearchDestinations.resolve(request)).containsExactly("JP");
    }

    @Test
    @DisplayName("destination/sector 둘 다 없으면 빈 목록이다(생략 신호)")
    void resolve_returnsEmpty_whenNeitherGiven() {
        FlightSearchRequestDto request = request(null, null);

        assertThat(FlightSearchDestinations.resolve(request)).isEmpty();
    }

    @Test
    @DisplayName("국가 목록이 있으면 전체(null) 조회를 하나 더 얹는다")
    void withAggregateIfBudgetAllows_appendsNull_whenDestinationsNonEmpty() {
        assertThat(FlightSearchDestinations.withAggregateIfBudgetAllows(List.of("JP", "TH"), 1))
                .containsExactly("JP", "TH", null);
    }

    @Test
    @DisplayName("이미 빈 목록(전체 상태)이면 그대로 둔다")
    void withAggregateIfBudgetAllows_keepsEmpty_whenAlreadyAggregate() {
        assertThat(FlightSearchDestinations.withAggregateIfBudgetAllows(List.of(), 1))
                .isEmpty();
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
}
