package com.restroute.flight.scheduler;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.cache.FlightCityNameCache;
import com.restroute.flight.cache.FlightCountryNameCache;
import com.restroute.flight.repository.FlightAirlineRepository;
import com.restroute.flight.repository.FlightAirportRepository;
import com.restroute.flight.repository.FlightCityRepository;
import com.restroute.flight.repository.FlightCountryRepository;
import com.restroute.flight.service.FlightReferenceDataSeeder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 국가/도시/공항/항공사 4종 참조 데이터의 시작 시 재시딩 + 캐시 채우기. 4종 모두 흐름은
 * 동일하고 SQL 경로·repository 콜백·캐시·로그 라벨만 달라({@link ReferenceDataSyncSpec}
 * 참고), 도메인별로 별도 클래스를 두는 대신 spec 목록을 순회한다. 도메인별
 * {@code flight.<domain>.sync.startup-enabled} 프로퍼티(기본 true)가 꺼져 있으면 그 spec만
 * 건너뛴다 — 재시딩도 캐시 refresh도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlightReferenceDataStartupInitializer implements ApplicationRunner {

    private final FlightReferenceDataSeeder flightReferenceDataSeeder;
    private final Environment environment;
    private final FlightAirlineRepository flightAirlineRepository;
    private final FlightAirlineNameCache flightAirlineNameCache;
    private final FlightAirportRepository flightAirportRepository;
    private final FlightAirportNameCache flightAirportNameCache;
    private final FlightCityRepository flightCityRepository;
    private final FlightCityNameCache flightCityNameCache;
    private final FlightCountryRepository flightCountryRepository;
    private final FlightCountryNameCache flightCountryNameCache;

    @Override
    public void run(ApplicationArguments args) {
        specs().forEach(this::runSpec);
    }

    private List<ReferenceDataSyncSpec> specs() {
        return List.of(
                ReferenceDataSyncSpec.of(
                        "airline",
                        "data/flight-airline-seed.sql",
                        flightAirlineRepository::deleteAllInBatch,
                        flightAirlineRepository::count,
                        flightAirlineNameCache::refresh,
                        "flight.airline.sync.startup-enabled"),
                ReferenceDataSyncSpec.of(
                        "airport",
                        "data/flight-airport-seed.sql",
                        flightAirportRepository::deleteAllInBatch,
                        flightAirportRepository::count,
                        flightAirportNameCache::refresh,
                        "flight.airport.sync.startup-enabled"),
                ReferenceDataSyncSpec.of(
                        "city",
                        "data/flight-city-seed.sql",
                        flightCityRepository::deleteAllInBatch,
                        flightCityRepository::count,
                        flightCityNameCache::refresh,
                        "flight.city.sync.startup-enabled"),
                ReferenceDataSyncSpec.of(
                        "country",
                        "data/flight-country-seed.sql",
                        flightCountryRepository::deleteAllInBatch,
                        flightCountryRepository::count,
                        flightCountryNameCache::refresh,
                        "flight.country.sync.startup-enabled"));
    }

    private void runSpec(ReferenceDataSyncSpec spec) {
        if (!environment.getProperty(spec.enabledPropertyKey(), Boolean.class, true)) {
            return;
        }
        try {
            int savedCount =
                    flightReferenceDataSeeder.reseed(spec.seedSqlPath(), spec.clearExisting(), spec.currentCount());
            log.info("Initial flight {} seeding completed. savedCount={}", spec.label(), savedCount);
        } catch (RuntimeException e) {
            log.error("Initial flight {} seeding failed. cause={}", spec.label(), e.getMessage(), e);
        }
        spec.refreshCache().run();
    }
}
