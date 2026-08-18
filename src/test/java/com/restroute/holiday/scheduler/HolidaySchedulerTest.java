package com.restroute.flight.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.service.FlightHolidaySyncService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightHolidaySchedulerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-18T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private FlightHolidaySyncService flightHolidaySyncService;

    @Test
    @DisplayName("올해와 내년 공휴일을 동기화한다")
    void syncHolidaysDaily_syncsThisYearAndNextYear() {
        FlightHolidayScheduler scheduler = new FlightHolidayScheduler(flightHolidaySyncService, CLOCK);
        when(flightHolidaySyncService.syncYear(2026)).thenReturn(2);
        when(flightHolidaySyncService.syncYear(2027)).thenReturn(0);

        scheduler.syncHolidaysDaily();

        verify(flightHolidaySyncService).syncYear(2026);
        verify(flightHolidaySyncService).syncYear(2027);
    }

    @Test
    @DisplayName("동기화가 실패해도 예외를 전파하지 않는다")
    void syncHolidaysDaily_doesNotPropagateFailure() {
        FlightHolidayScheduler scheduler = new FlightHolidayScheduler(flightHolidaySyncService, CLOCK);
        when(flightHolidaySyncService.syncYear(2026)).thenThrow(new RuntimeException("boom"));
        when(flightHolidaySyncService.syncYear(2027)).thenReturn(1);

        assertThatCode(scheduler::syncHolidaysDaily).doesNotThrowAnyException();

        verify(flightHolidaySyncService).syncYear(2027);
    }
}
