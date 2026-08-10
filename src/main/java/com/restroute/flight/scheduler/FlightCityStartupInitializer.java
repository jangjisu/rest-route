package com.restroute.flight.scheduler;

import com.restroute.flight.service.FlightCitySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "flight.city.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightCityStartupInitializer implements ApplicationRunner {

    private final FlightCitySyncService flightCitySyncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int savedCount = flightCitySyncService.initializeFlightCitiesIfEmpty();
            if (savedCount > 0) {
                log.info("Initial flight city sync completed. savedCount={}", savedCount);
                return;
            }

            log.info("Initial flight city sync skipped because flight_city table already has data.");
        } catch (RuntimeException e) {
            log.error("Initial flight city sync failed. cause={}", e.getMessage(), e);
        }
    }
}
