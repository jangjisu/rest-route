package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightAirlineEntity;
import com.restroute.flight.repository.FlightAirlineRepository;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * code→항공사명 조회를 위한 순수 인메모리 캐시. DB 왕복 없이 딜 응답 조립 시 읽기 전용으로 쓰인다.
 * 시작 시 1회, 동기화 갱신 직후 1회 {@link #refresh()}로 다시 채워진다.
 */
@Component
@RequiredArgsConstructor
public class FlightAirlineNameCache {

    private final FlightAirlineRepository flightAirlineRepository;

    private volatile Map<String, String> nameByCode = Map.of();

    public String findName(String code) {
        return nameByCode.get(code);
    }

    public void refresh() {
        nameByCode = flightAirlineRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        FlightAirlineEntity::getCode, FlightAirlineNameCache::displayName));
    }

    private static String displayName(FlightAirlineEntity entity) {
        return entity.getKorName() != null ? entity.getKorName() : entity.getEngName();
    }
}
