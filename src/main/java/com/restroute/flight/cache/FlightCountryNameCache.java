package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightCountryEntity;
import com.restroute.flight.repository.FlightCountryRepository;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * code→국가명 조회를 위한 순수 인메모리 캐시. DB 왕복 없이 딜 응답 조립 시 읽기 전용으로 쓰인다.
 * 시작 시 1회, 동기화 갱신 직후 1회 {@link #refresh()}로 다시 채워진다.
 */
@Component
@RequiredArgsConstructor
public class FlightCountryNameCache {

    private final FlightCountryRepository flightCountryRepository;

    private volatile Map<String, String> nameByCode = Map.of();

    public String findName(String code) {
        return nameByCode.get(code);
    }

    public void refresh() {
        nameByCode = flightCountryRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        FlightCountryEntity::getCode, FlightCountryNameCache::displayName));
    }

    private static String displayName(FlightCountryEntity entity) {
        return entity.getKorName() != null ? entity.getKorName() : entity.getEngName();
    }
}
