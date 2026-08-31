package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightCityEntity;
import com.restroute.flight.repository.FlightCityRepository;
import org.springframework.stereotype.Component;

/** code→도시명 조회 캐시 — 공통 동작은 {@link ReferenceDataNameCache}. */
@Component
public class FlightCityNameCache extends ReferenceDataNameCache<FlightCityEntity> {

    public FlightCityNameCache(FlightCityRepository flightCityRepository) {
        super(
                flightCityRepository::findAll,
                FlightCityEntity::getCode,
                FlightCityEntity::getKorName,
                FlightCityEntity::getEngName);
    }
}
