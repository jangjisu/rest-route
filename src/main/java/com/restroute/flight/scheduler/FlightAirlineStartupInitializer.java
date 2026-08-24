package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.repository.FlightAirlineRepository;
import com.restroute.flight.service.FlightReferenceDataSeeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "flight.airline.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightAirlineStartupInitializer extends ReferenceDataStartupInitializer {

    private final FlightAirlineNameCache flightAirlineNameCache;

    public FlightAirlineStartupInitializer(
            FlightReferenceDataSeeder flightReferenceDataSeeder,
            FlightAirlineRepository flightAirlineRepository,
            FlightAirlineNameCache flightAirlineNameCache) {
        super(
                flightReferenceDataSeeder,
                "data/flight-airline-seed.sql",
                flightAirlineRepository::deleteAllInBatch,
                flightAirlineRepository::count);
        this.flightAirlineNameCache = flightAirlineNameCache;
    }

    @Override
    protected void onSeedSuccess(int savedCount) {
        log.info("Initial flight airline seeding completed. savedCount={}", savedCount);
    }

    @Override
    protected void onSeedFailure(RuntimeException e) {
        log.error("Initial flight airline seeding failed. cause={}", e.getMessage(), e);
    }

    @Override
    protected void refreshCache() {
        flightAirlineNameCache.refresh();
    }
}
