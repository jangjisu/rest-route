package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightCityNameCache;
import com.restroute.flight.repository.FlightCityRepository;
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
        prefix = "flight.city.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightCityStartupInitializer implements ApplicationRunner {

    private static final String SEED_SQL_PATH = "data/flight-city-seed.sql";

    private final FlightReferenceDataSeeder flightReferenceDataSeeder;
    private final FlightCityRepository flightCityRepository;
    private final FlightCityNameCache flightCityNameCache;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int savedCount = flightReferenceDataSeeder.reseed(
                    SEED_SQL_PATH, flightCityRepository::deleteAllInBatch, flightCityRepository::count);
            log.info("Initial flight city seeding completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Initial flight city seeding failed. cause={}", e.getMessage(), e);
        }
        flightCityNameCache.refresh();
    }
}
