package com.restroute.flight.scheduler;

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
 * 국가/도시/공항/항공사 4종 참조 데이터의 시작 시 재시딩. 4종 모두 흐름은 동일하고 SQL
 * 경로·repository 콜백·로그 라벨만 달라({@link ReferenceDataSyncSpec} 참고), 도메인별로 별도
 * 클래스를 두는 대신 spec 목록을 순회한다. 도메인별 {@code flight.<domain>.sync.startup-enabled}
 * 프로퍼티(기본 true)가 꺼져 있으면 그 spec만 건너뛴다.
 *
 * <p>재시딩된 데이터는 인메모리 캐시로 올리지 않는다 — 항공권 검색이 딜을 조립할 때 공항·항공사
 * 이름이 필요하면 그때그때 DB를 직접 조회한다({@link com.restroute.flight.service.FlightDealResponseMapper}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlightReferenceDataStartupInitializer implements ApplicationRunner {

    private final FlightReferenceDataSeeder flightReferenceDataSeeder;
    private final Environment environment;
    private final FlightAirlineRepository flightAirlineRepository;
    private final FlightAirportRepository flightAirportRepository;
    private final FlightCityRepository flightCityRepository;
    private final FlightCountryRepository flightCountryRepository;

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
                        "flight.airline.sync.startup-enabled"),
                ReferenceDataSyncSpec.of(
                        "airport",
                        "data/flight-airport-seed.sql",
                        flightAirportRepository::deleteAllInBatch,
                        flightAirportRepository::count,
                        "flight.airport.sync.startup-enabled"),
                ReferenceDataSyncSpec.of(
                        "city",
                        "data/flight-city-seed.sql",
                        flightCityRepository::deleteAllInBatch,
                        flightCityRepository::count,
                        "flight.city.sync.startup-enabled"),
                ReferenceDataSyncSpec.of(
                        "country",
                        "data/flight-country-seed.sql",
                        flightCountryRepository::deleteAllInBatch,
                        flightCountryRepository::count,
                        "flight.country.sync.startup-enabled"));
    }

    private void runSpec(ReferenceDataSyncSpec spec) {
        if (!isEnabled(spec)) {
            return;
        }
        try {
            int savedCount =
                    flightReferenceDataSeeder.reseed(spec.seedSqlPath(), spec.clearExisting(), spec.currentCount());
            log.info("Initial flight {} seeding completed. savedCount={}", spec.label(), savedCount);
        } catch (RuntimeException e) {
            log.error("Initial flight {} seeding failed. cause={}", spec.label(), e.getMessage(), e);
        }
    }

    /**
     * 예전 @ConditionalOnProperty(havingValue = "true")와 같은 방식으로 판정한다 — "true"가
     * 아니면(오타·이상한 값 포함) 전부 꺼진 것으로 본다. Environment.getProperty(key,
     * Boolean.class, true)는 값이 "true"/"false"로 인식되지 않으면 변환 예외를 던져 run()
     * 전체를 중단시킬 수 있어서 쓰지 않는다.
     */
    private boolean isEnabled(ReferenceDataSyncSpec spec) {
        return Boolean.parseBoolean(environment.getProperty(spec.enabledPropertyKey(), "true"));
    }
}
