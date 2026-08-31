package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightAirportEntity;
import com.restroute.flight.repository.FlightAirportRepository;
import org.springframework.stereotype.Component;

/** code→공항명 조회 캐시 — 공통 동작은 {@link ReferenceDataNameCache}. */
@Component
public class FlightAirportNameCache extends ReferenceDataNameCache<FlightAirportEntity> {

    public FlightAirportNameCache(FlightAirportRepository flightAirportRepository) {
        super(
                flightAirportRepository::findAll,
                FlightAirportEntity::getCode,
                FlightAirportEntity::getKorName,
                FlightAirportEntity::getEngName);
    }
}
