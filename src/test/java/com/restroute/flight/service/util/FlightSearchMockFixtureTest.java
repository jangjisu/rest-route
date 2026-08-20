package com.restroute.flight.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.holiday.domain.HolidayEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchMockFixtureTest {

    private static String futureDate(int daysFromToday) {
        return LocalDate.now().plusDays(daysFromToday).toString();
    }

    @Test
    @DisplayName("includeWeekend가 false(기본값)면 실 경로와 동일하게 주말 출발 항목을 뺀다")
    void generateAll_excludesWeekendDepartures_whenIncludeWeekendFalse() {
        FlightSearchRequestDto request = request(null);

        List<FlightDealResponse> items = FlightSearchMockFixture.generateAll(request, "tok1", 77);

        assertThat(items).isNotEmpty();
        assertThat(items)
                .allSatisfy(item -> assertThat(HolidayEntity.isWeekend(FlightDealResponses.departureDateOf(item)))
                        .isFalse());
    }

    @Test
    @DisplayName("includeWeekend=true면 주말 출발 항목도 포함하고, 그 날짜는 holidays에 이름 없이 표시된다")
    void generateAll_includesWeekendDepartures_whenIncludeWeekendTrue() {
        FlightSearchRequestDto request = request("true");

        List<FlightDealResponse> items = FlightSearchMockFixture.generateAll(request, "tok1", 77);

        assertThat(items).hasSize(77);
        FlightDealResponse weekendItem = items.stream()
                .filter(item -> HolidayEntity.isWeekend(FlightDealResponses.departureDateOf(item)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("15일 범위엔 주말 출발이 최소 하나는 있어야 한다"));

        String departureDate = FlightDealResponses.departureDateOf(weekendItem).toString();
        assertThat(weekendItem.holidays()).contains(new FlightDealResponse.HolidayDay(departureDate, null));
    }

    private static FlightSearchRequestDto request(String includeWeekend) {
        return new FlightSearchRequestDto(
                "ICN",
                "range",
                futureDate(10),
                futureDate(24),
                null,
                List.of("3"),
                null,
                includeWeekend,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
