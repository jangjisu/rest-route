package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightCountryNameCache;
import com.restroute.flight.repository.FlightCountryRepository;
import com.restroute.flight.service.FlightReferenceDataSeeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "flight.country.sync",
        name = "startup-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FlightCountryStartupInitializer extends ReferenceDataStartupInitializer {

    private final FlightCountryNameCache flightCountryNameCache;

    public FlightCountryStartupInitializer(
            FlightReferenceDataSeeder flightReferenceDataSeeder,
            FlightCountryRepository flightCountryRepository,
            FlightCountryNameCache flightCountryNameCache) {
        super(
                flightReferenceDataSeeder,
                "data/flight-country-seed.sql",
                flightCountryRepository::deleteAllInBatch,
                flightCountryRepository::count);
        this.flightCountryNameCache = flightCountryNameCache;
    }

    @Override
    protected void onSeedSuccess(int savedCount) {
        log.info("Initial flight country seeding completed. savedCount={}", savedCount);
    }

    @Override
    protected void onSeedFailure(RuntimeException e) {
        log.error("Initial flight country seeding failed. cause={}", e.getMessage(), e);
    }

    @Override
    protected void refreshCache() {
        flightCountryNameCache.refresh();
    }
}
