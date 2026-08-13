package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.repository.FlightAirportRepository;
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
        prefix = "flight.airport.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightAirportStartupInitializer implements ApplicationRunner {

    private static final String SEED_SQL_PATH = "data/flight-airport-seed.sql";

    private final FlightReferenceDataSeeder flightReferenceDataSeeder;
    private final FlightAirportRepository flightAirportRepository;
    private final FlightAirportNameCache flightAirportNameCache;

    @Override
    public void run(ApplicationArguments args) {
        try {
            logStartupResult(flightReferenceDataSeeder.seedIfEmpty(SEED_SQL_PATH, flightAirportRepository::count));
        } catch (RuntimeException e) {
            log.error("Initial flight airport seeding failed. cause={}", e.getMessage(), e);
        }
        flightAirportNameCache.refresh();
    }

    private static void logStartupResult(int savedCount) {
        if (savedCount > 0) {
            log.info("Initial flight airport seeding completed. savedCount={}", savedCount);
            return;
        }
        log.info("Initial flight airport seeding skipped because flight_airport table already has data.");
    }
}
