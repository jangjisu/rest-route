package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.SpecialDayClient;
import com.restroute.flight.client.response.SpecialDayResponse;
import com.restroute.flight.domain.FlightHolidayEntity;
import com.restroute.flight.repository.FlightHolidayRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightHolidaySyncServiceTest {

    @Mock
    private SpecialDayClient specialDayClient;

    @Mock
    private FlightHolidayRepository flightHolidayRepository;

    private FlightHolidaySyncService service;

    @BeforeEach
    void setUp() {
        service = new FlightHolidaySyncService(specialDayClient, flightHolidayRepository);
    }

    @Test
    @DisplayName("아직 등록되지 않은 실제 공휴일만 저장하고 저장 건수를 반환한다")
    void syncYear_savesOnlyNewActualHolidays() {
        SpecialDayResponse.Item liberationDay = new SpecialDayResponse.Item("20260815", "광복절", "Y");
        SpecialDayResponse.Item alreadyRegistered = new SpecialDayResponse.Item("20260101", "신정", "Y");
        SpecialDayResponse.Item notActuallyOff = new SpecialDayResponse.Item("20260706", "제헌절", "N");
        when(specialDayClient.restDaysOfYear(2026))
                .thenReturn(List.of(liberationDay, alreadyRegistered, notActuallyOff));
        when(flightHolidayRepository.existsByHolidayDate(LocalDate.of(2026, 8, 15)))
                .thenReturn(false);
        when(flightHolidayRepository.existsByHolidayDate(LocalDate.of(2026, 1, 1)))
                .thenReturn(true);

        int savedCount = service.syncYear(2026);

        assertThat(savedCount).isEqualTo(1);
        ArgumentCaptor<FlightHolidayEntity> captor = ArgumentCaptor.forClass(FlightHolidayEntity.class);
        verify(flightHolidayRepository).save(captor.capture());
        assertThat(captor.getValue().getHolidayDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(captor.getValue().getName()).isEqualTo("광복절");
    }

    @Test
    @DisplayName("공공기관 휴일이 아닌(isHoliday=N) 항목은 저장하지 않는다")
    void syncYear_skipsNonHolidayItems() {
        when(specialDayClient.restDaysOfYear(2026))
                .thenReturn(List.of(new SpecialDayResponse.Item("20260706", "제헌절", "N")));

        int savedCount = service.syncYear(2026);

        assertThat(savedCount).isEqualTo(0);
        verify(flightHolidayRepository, never()).save(any());
    }
}
