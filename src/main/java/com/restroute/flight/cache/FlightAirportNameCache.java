package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightAirportEntity;
import com.restroute.flight.repository.FlightAirportRepository;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * code→공항명 조회를 위한 순수 인메모리 캐시. DB 왕복 없이 딜 응답 조립 시 읽기 전용으로 쓰인다.
 * 시작 시 1회 {@link #refresh()}로 채워진다.
 */
@Component
@RequiredArgsConstructor
public class FlightAirportNameCache {

    private final FlightAirportRepository flightAirportRepository;

    private volatile Map<String, String> nameByCode = Map.of();

    public String findName(String code) {
        return nameByCode.get(code);
    }

    public void refresh() {
        nameByCode = flightAirportRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        FlightAirportEntity::getCode, FlightAirportNameCache::displayName));
    }

    private static String displayName(FlightAirportEntity entity) {
        return entity.getKorName() != null ? entity.getKorName() : entity.getEngName();
    }
}
