package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightCountryEntity;
import com.restroute.flight.repository.FlightCountryRepository;
import org.springframework.stereotype.Component;

/** code→국가명 조회 캐시 — 공통 동작은 {@link ReferenceDataNameCache}. */
@Component
public class FlightCountryNameCache extends ReferenceDataNameCache<FlightCountryEntity> {

    public FlightCountryNameCache(FlightCountryRepository flightCountryRepository) {
        super(
                flightCountryRepository::findAll,
                FlightCountryEntity::getCode,
                FlightCountryEntity::getKorName,
                FlightCountryEntity::getEngName);
    }
}
