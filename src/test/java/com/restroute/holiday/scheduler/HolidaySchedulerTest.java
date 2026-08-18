package com.restroute.holiday.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.holiday.service.HolidaySyncService;
import com.restroute.holiday.service.dto.HolidaySyncResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HolidaySchedulerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-18T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private HolidaySyncService holidaySyncService;

    @Test
    @DisplayName("올해와 내년 공휴일을 동기화한다")
    void syncHolidaysDaily_syncsThisYearAndNextYear() {
        HolidayScheduler scheduler = new HolidayScheduler(holidaySyncService, CLOCK);
        when(holidaySyncService.syncYear(2026)).thenReturn(HolidaySyncResult.of(2, 0));
        when(holidaySyncService.syncYear(2027)).thenReturn(HolidaySyncResult.of(0, 1));

        scheduler.syncHolidaysDaily();

        verify(holidaySyncService).syncYear(2026);
        verify(holidaySyncService).syncYear(2027);
    }

    @Test
    @DisplayName("동기화가 실패해도 예외를 전파하지 않는다")
    void syncHolidaysDaily_doesNotPropagateFailure() {
        HolidayScheduler scheduler = new HolidayScheduler(holidaySyncService, CLOCK);
        when(holidaySyncService.syncYear(2026)).thenThrow(new RuntimeException("boom"));
        when(holidaySyncService.syncYear(2027)).thenReturn(HolidaySyncResult.of(1, 0));

        assertThatCode(scheduler::syncHolidaysDaily).doesNotThrowAnyException();

        verify(holidaySyncService).syncYear(2027);
    }
}
