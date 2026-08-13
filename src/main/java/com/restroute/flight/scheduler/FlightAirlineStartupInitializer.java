package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.repository.FlightAirlineRepository;
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
        prefix = "flight.airline.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightAirlineStartupInitializer implements ApplicationRunner {

    private static final String SEED_SQL_PATH = "data/flight-airline-seed.sql";

    private final FlightReferenceDataSeeder flightReferenceDataSeeder;
    private final FlightAirlineRepository flightAirlineRepository;
    private final FlightAirlineNameCache flightAirlineNameCache;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int savedCount = flightReferenceDataSeeder.reseed(
                    SEED_SQL_PATH, flightAirlineRepository::deleteAllInBatch, flightAirlineRepository::count);
            log.info("Initial flight airline seeding completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Initial flight airline seeding failed. cause={}", e.getMessage(), e);
        }
        flightAirlineNameCache.refresh();
    }
}
