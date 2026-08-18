package com.restroute.flight.scheduler;

import com.restroute.flight.service.FlightHolidaySyncService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 올해와 내년 공휴일을 매일 동기화한다 — 대체공휴일은 관보 고시가 늦어질 수 있어서 매일 다시
 * 확인하고, 내년 것까지 미리 받아두면 연도가 바뀌기 전에도 반영된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlightHolidayScheduler {

    private final FlightHolidaySyncService flightHolidaySyncService;
    private final Clock clock;

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Seoul")
    public void syncHolidaysDaily() {
        int year = LocalDate.now(clock).getYear();
        syncYear(year);
        syncYear(year + 1);
    }

    private void syncYear(int year) {
        try {
            int savedCount = flightHolidaySyncService.syncYear(year);
            log.info("Scheduled flight holiday sync completed. year={}, savedCount={}", year, savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled flight holiday sync failed. year={}, cause={}", year, e.getMessage(), e);
        }
    }
}
