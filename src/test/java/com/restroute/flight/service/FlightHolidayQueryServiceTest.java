package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.controller.response.FlightHolidayResponse;
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
class FlightHolidayQueryServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    private FlightHolidayQueryService flightHolidayQueryService;

    @BeforeEach
    void setUp() {
        flightHolidayQueryService = new FlightHolidayQueryService(holidayRepository);
    }

    @Test
    @DisplayName("months가 없으면 전체 공휴일을 조회한다")
    void findAll_returnsAll_whenMonthsMissing() {
        HolidayEntity newYear = HolidayEntity.syncedFromApi(LocalDate.of(2026, 1, 1), "신정");
        when(holidayRepository.findAllByOrderByHolidayDateAsc()).thenReturn(List.of(newYear));

        List<FlightHolidayResponse> result = flightHolidayQueryService.findAll(null);

        assertThat(result).containsExactly(new FlightHolidayResponse("2026-01-01", "신정"));
        verify(holidayRepository, never()).findAllByMonthInOrderByHolidayDateAsc(any());
    }

    @Test
    @DisplayName("months가 빈 목록이어도 전체 공휴일을 조회한다")
    void findAll_returnsAll_whenMonthsEmpty() {
        when(holidayRepository.findAllByOrderByHolidayDateAsc()).thenReturn(List.of());

        List<FlightHolidayResponse> result = flightHolidayQueryService.findAll(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("months가 있으면 그 달들만 조회한다")
    void findAll_filtersByMonths_whenMonthsGiven() {
        HolidayEntity chuseok = HolidayEntity.syncedFromApi(LocalDate.of(2026, 9, 25), "추석");
        when(holidayRepository.findAllByMonthInOrderByHolidayDateAsc(List.of(9, 10)))
                .thenReturn(List.of(chuseok));

        List<FlightHolidayResponse> result = flightHolidayQueryService.findAll(List.of(9, 10));

        assertThat(result).containsExactly(new FlightHolidayResponse("2026-09-25", "추석"));
    }
}
