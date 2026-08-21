package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.holiday.repository.HolidayRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightDealPostFilterTest {

    private static final LocalDate DATE_FROM = LocalDate.now().plusDays(10);
    private static final LocalDate DATE_TO = DATE_FROM.plusDays(20);
    private static final LocalDate SATURDAY = nextDayOfWeek(DayOfWeek.SATURDAY);
    private static final LocalDate MONDAY = nextDayOfWeek(DayOfWeek.MONDAY);

    @Mock
    private HolidayRepository holidayRepository;

    private FlightDealPostFilter filter;

    @BeforeEach
    void setUp() {
        filter = new FlightDealPostFilter(holidayRepository);
    }

    /** DATE_FROM~DATE_TO(20일 폭)는 항상 토요일·월요일을 최소 한 번씩 포함한다. */
    private static LocalDate nextDayOfWeek(DayOfWeek target) {
        LocalDate date = DATE_FROM;
        while (date.getDayOfWeek() != target) {
            date = date.plusDays(1);
        }
        return date;
    }

    private static FlightDealResponse dealAt(
            String id, LocalDate departDate, int departureTransfers, int arrivalTransfers) {
        FlightDealResponse.Leg departure = new FlightDealResponse.Leg(
                departDate + "T09:00:00+09:00", departDate + "T10:30:00+09:00", 90, departureTransfers);
        FlightDealResponse.Leg arrival = new FlightDealResponse.Leg(
                departDate + "T13:00:00+09:00", departDate + "T14:30:00+09:00", 90, arrivalTransfers);
        return new FlightDealResponse(
                id,
                new FlightDealResponse.Destination("KIX", "오사카"),
                departure,
                arrival,
                3,
                List.of(),
                new FlightDealResponse.Airline("LJ", "진에어", false),
                new FlightDealResponse.Price(89000, "KRW"),
                false,
                "Aviasales",
                "link",
                null);
    }

    private static FlightSearchRequestDto request(
            String includeWeekend, String includeHoliday, String includeTransfer) {
        return new FlightSearchRequestDto(
                "ICN",
                "range",
                DATE_FROM.toString(),
                DATE_TO.toString(),
                null,
                List.of("3"),
                null,
                includeWeekend,
                includeHoliday,
                includeTransfer,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    @DisplayName("실제 요청한 dateFrom~dateTo 밖의 출발일은 항상 뺀다")
    void apply_removesItemsOutsideRequestedDateRange() {
        List<FlightDealResponse> items = List.of(
                dealAt("BEFORE", DATE_FROM.minusDays(1), 0, 0),
                dealAt("IN_RANGE", MONDAY, 0, 0),
                dealAt("AFTER", DATE_TO.plusDays(1), 0, 0));

        List<FlightDealResponse> result = filter.apply(items, request("true", "true", null));

        assertThat(result).extracting(FlightDealResponse::id).containsExactly("IN_RANGE");
    }

    @Test
    @DisplayName("includeTransfer=false면 경유가 하나라도 있는 항목을 뺀다")
    void apply_removesTransfersWhenIncludeTransferFalse() {
        List<FlightDealResponse> items =
                List.of(dealAt("A", MONDAY, 0, 0), dealAt("B", MONDAY, 1, 0), dealAt("C", MONDAY, 0, 1));

        List<FlightDealResponse> result = filter.apply(items, request(null, null, "false"));

        assertThat(result).extracting(FlightDealResponse::id).containsExactly("A");
    }

    @Test
    @DisplayName("includeTransfer가 없으면(기본 포함) 경유 항목도 그대로 둔다")
    void apply_keepsTransfersByDefault() {
        List<FlightDealResponse> items = List.of(dealAt("A", MONDAY, 1, 0));

        List<FlightDealResponse> result = filter.apply(items, request(null, null, null));

        assertThat(result).extracting(FlightDealResponse::id).containsExactly("A");
    }

    @Test
    @DisplayName("includeWeekend가 없으면(기본 제외) 출발일이 주말인 항목을 뺀다")
    void apply_removesWeekendDeparturesByDefault() {
        List<FlightDealResponse> items = List.of(dealAt("SAT", SATURDAY, 0, 0), dealAt("MON", MONDAY, 0, 0));

        List<FlightDealResponse> result = filter.apply(items, request(null, null, null));

        assertThat(result).extracting(FlightDealResponse::id).containsExactly("MON");
    }

    @Test
    @DisplayName("includeWeekend=true면 출발일이 주말인 항목도 남긴다")
    void apply_keepsWeekendDepartures_whenExplicitlyIncluded() {
        List<FlightDealResponse> items = List.of(dealAt("SAT", SATURDAY, 0, 0));

        List<FlightDealResponse> result = filter.apply(items, request("true", null, null));

        assertThat(result).extracting(FlightDealResponse::id).containsExactly("SAT");
    }

    @Test
    @DisplayName("includeHoliday가 없으면(기본 제외) 출발일이 공휴일인 항목을 뺀다")
    void apply_removesHolidayDepartures_byDefault() {
        // MONDAY 다음날(화요일)은 항상 평일이라 다른 필터에 안 걸린다.
        LocalDate normalDay = MONDAY.plusDays(1);
        List<FlightDealResponse> items = List.of(dealAt("HOL", MONDAY, 0, 0), dealAt("NORMAL", normalDay, 0, 0));
        when(holidayRepository.findHolidayDatesBetween(MONDAY, normalDay)).thenReturn(List.of(MONDAY));

        List<FlightDealResponse> result = filter.apply(items, request("true", null, null));

        assertThat(result).extracting(FlightDealResponse::id).containsExactly("NORMAL");
    }

    @Test
    @DisplayName("includeHoliday=true면 공휴일 조회 자체를 하지 않는다")
    void apply_skipsHolidayLookup_whenIncludeHolidayTrue() {
        List<FlightDealResponse> items = List.of(dealAt("A", MONDAY, 0, 0));

        filter.apply(items, request("true", "true", null));

        verify(holidayRepository, never()).findHolidayDatesBetween(any(), any());
    }

    @Test
    @DisplayName("빈 목록이면 아무 필터도 예외 없이 빈 목록을 반환한다")
    void apply_handlesEmptyList() {
        List<FlightDealResponse> result = filter.apply(List.of(), request(null, null, null));

        assertThat(result).isEmpty();
    }
}
