package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightCountryNameCache;
import com.restroute.flight.repository.FlightCountryRepository;
import com.restroute.flight.service.FlightReferenceDataSeeder;
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
        prefix = "flight.country.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightCountryStartupInitializer implements ApplicationRunner {

    private static final String SEED_SQL_PATH = "data/flight-country-seed.sql";

    private final FlightReferenceDataSeeder flightReferenceDataSeeder;
    private final FlightCountryRepository flightCountryRepository;
    private final FlightCountryNameCache flightCountryNameCache;

    @Override
    public void run(ApplicationArguments args) {
        try {
            logStartupResult(flightReferenceDataSeeder.seedIfEmpty(SEED_SQL_PATH, flightCountryRepository::count));
        } catch (RuntimeException e) {
            log.error("Initial flight country seeding failed. cause={}", e.getMessage(), e);
        }
        flightCountryNameCache.refresh();
    }

    private static void logStartupResult(int savedCount) {
        if (savedCount > 0) {
            log.info("Initial flight country seeding completed. savedCount={}", savedCount);
            return;
        }
        log.info("Initial flight country seeding skipped because flight_country table already has data.");
    }
}
