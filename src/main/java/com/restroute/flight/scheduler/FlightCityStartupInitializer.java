package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightCityNameCache;
import com.restroute.flight.repository.FlightCityRepository;
import com.restroute.flight.service.FlightReferenceDataSeeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "flight.city.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightCityStartupInitializer extends ReferenceDataStartupInitializer {

    private final FlightCityNameCache flightCityNameCache;

    public FlightCityStartupInitializer(
            FlightReferenceDataSeeder flightReferenceDataSeeder,
            FlightCityRepository flightCityRepository,
            FlightCityNameCache flightCityNameCache) {
        super(
                flightReferenceDataSeeder,
                "data/flight-city-seed.sql",
                flightCityRepository::deleteAllInBatch,
                flightCityRepository::count);
        this.flightCityNameCache = flightCityNameCache;
    }

    @Override
    protected void onSeedSuccess(int savedCount) {
        log.info("Initial flight city seeding completed. savedCount={}", savedCount);
    }

    @Override
    protected void onSeedFailure(RuntimeException e) {
        log.error("Initial flight city seeding failed. cause={}", e.getMessage(), e);
    }

    @Override
    protected void refreshCache() {
        flightCityNameCache.refresh();
    }
}
