package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightCountryEntity;
import com.restroute.flight.repository.FlightCountryRepository;
import org.springframework.stereotype.Component;

/**
 * code→국가명 조회를 위한 순수 인메모리 캐시. DB 왕복 없이 딜 응답 조립 시 읽기 전용으로 쓰인다.
 * 시작 시 1회, 동기화 갱신 직후 1회 {@link #refresh()}로 다시 채워진다.
 */
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
