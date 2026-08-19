package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightDealHolidayEnricherTest {

    @Mock
    private HolidayRepository holidayRepository;

    private FlightDealHolidayEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new FlightDealHolidayEnricher(holidayRepository);
    }

    /** 2026-09-19(토)~2026-09-22(화), 3박. 2026-09-21(월)은 추석 공휴일로 세팅해둔다. */
    private static FlightDealResponse dealAt(String departDate, String returnDate) {
        FlightDealResponse.Leg departure =
                new FlightDealResponse.Leg(departDate + "T09:00:00+09:00", departDate + "T10:30:00+09:00", 90, 0);
        FlightDealResponse.Leg arrival =
                new FlightDealResponse.Leg(returnDate + "T13:00:00+09:00", returnDate + "T14:30:00+09:00", 90, 0);
        return new FlightDealResponse(
                "T_0001",
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

    @Test
    @DisplayName("빈 목록이면 조회 없이 그대로 돌려준다")
    void enrich_returnsEmptyList_whenItemsEmpty() {
        List<FlightDealResponse> result = enricher.enrich(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("범위 안에 공휴일이 있으면 이름을 채운 항목을 추가한다")
    void enrich_addsHolidayEntry_withName() {
        FlightDealResponse deal = dealAt("2026-09-19", "2026-09-22");
        when(holidayRepository.findAllByHolidayDateBetween(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 22)))
                .thenReturn(List.of(HolidayEntity.syncedFromApi(LocalDate.of(2026, 9, 21), "추석")));

        List<FlightDealResponse> result = enricher.enrich(List.of(deal));

        assertThat(result.get(0).holidays()).contains(new FlightDealResponse.HolidayDay("2026-09-21", "추석"));
    }

    @Test
    @DisplayName("공휴일 테이블에 없는 순수 주말은 이름 없이 추가한다")
    void enrich_addsWeekendEntry_withoutName() {
        FlightDealResponse deal = dealAt("2026-09-19", "2026-09-22");
        when(holidayRepository.findAllByHolidayDateBetween(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 22)))
                .thenReturn(List.of());

        List<FlightDealResponse> result = enricher.enrich(List.of(deal));

        assertThat(result.get(0).holidays()).contains(new FlightDealResponse.HolidayDay("2026-09-19", null));
    }

    @Test
    @DisplayName("평일이면서 공휴일도 아닌 날은 목록에 포함하지 않는다")
    void enrich_excludesPlainWeekdays() {
        FlightDealResponse deal = dealAt("2026-09-19", "2026-09-22");
        when(holidayRepository.findAllByHolidayDateBetween(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 22)))
                .thenReturn(List.of());

        List<FlightDealResponse> result = enricher.enrich(List.of(deal));

        assertThat(result.get(0).holidays())
                .extracting(FlightDealResponse.HolidayDay::date)
                .doesNotContain("2026-09-22");
    }

    @Test
    @DisplayName("출발일과 귀국일 양끝을 모두 범위에 포함한다")
    void enrich_includesBothEndpoints() {
        FlightDealResponse deal = dealAt("2026-09-19", "2026-09-22");
        when(holidayRepository.findAllByHolidayDateBetween(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 22)))
                .thenReturn(List.of());

        List<FlightDealResponse> result = enricher.enrich(List.of(deal));

        assertThat(result.get(0).holidays())
                .extracting(FlightDealResponse.HolidayDay::date)
                .contains("2026-09-19");
    }

    @Test
    @DisplayName("여러 딜의 출발일~귀국일 범위를 모아 공휴일을 한 번만 조회한다")
    void enrich_queriesHolidayRepositoryOnce_acrossAllDeals() {
        FlightDealResponse first = dealAt("2026-09-19", "2026-09-22");
        FlightDealResponse second = dealAt("2026-10-01", "2026-10-04");
        when(holidayRepository.findAllByHolidayDateBetween(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 10, 4)))
                .thenReturn(List.of());

        enricher.enrich(List.of(first, second));

        verify(holidayRepository).findAllByHolidayDateBetween(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 10, 4));
    }
}
