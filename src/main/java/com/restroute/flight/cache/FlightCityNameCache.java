package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightCityEntity;
import com.restroute.flight.repository.FlightCityRepository;
import org.springframework.stereotype.Component;

/**
 * code→도시명 조회를 위한 순수 인메모리 캐시. DB 왕복 없이 딜 응답 조립 시 읽기 전용으로 쓰인다.
 * 시작 시 1회, 동기화 갱신 직후 1회 {@link #refresh()}로 다시 채워진다.
 */
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
