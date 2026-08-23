package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.repository.FlightAirportRepository;
import com.restroute.flight.service.FlightReferenceDataSeeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "flight.airport.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightAirportStartupInitializer extends ReferenceDataStartupInitializer {

    private final FlightAirportNameCache flightAirportNameCache;

    public FlightAirportStartupInitializer(
            FlightReferenceDataSeeder flightReferenceDataSeeder,
            FlightAirportRepository flightAirportRepository,
            FlightAirportNameCache flightAirportNameCache) {
        super(
                flightReferenceDataSeeder,
                "data/flight-airport-seed.sql",
                flightAirportRepository::deleteAllInBatch,
                flightAirportRepository::count);
        this.flightAirportNameCache = flightAirportNameCache;
    }

    @Override
    protected void onSeedSuccess(int savedCount) {
        log.info("Initial flight airport seeding completed. savedCount={}", savedCount);
    }

    @Override
    protected void onSeedFailure(RuntimeException e) {
        log.error("Initial flight airport seeding failed. cause={}", e.getMessage(), e);
    }

    @Override
    protected void refreshCache() {
        flightAirportNameCache.refresh();
    }
}
