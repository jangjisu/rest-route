package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightAirportEntity;
import com.restroute.flight.repository.FlightAirportRepository;
import org.springframework.stereotype.Component;

/**
 * code→공항명 조회를 위한 순수 인메모리 캐시. DB 왕복 없이 딜 응답 조립 시 읽기 전용으로 쓰인다.
 * 시작 시 1회 {@link #refresh()}로 채워진다.
 */
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
