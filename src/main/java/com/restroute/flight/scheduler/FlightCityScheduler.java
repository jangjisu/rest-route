package com.restroute.flight.scheduler;

import com.restroute.flight.service.FlightCitySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightCityScheduler {

    private final FlightCitySyncService flightCitySyncService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void syncFlightCitiesDaily() {
        try {
            int savedCount = flightCitySyncService.refreshFlightCities();
            log.info("Scheduled flight city sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled flight city sync failed. cause={}", e.getMessage(), e);
        }
    }
}
