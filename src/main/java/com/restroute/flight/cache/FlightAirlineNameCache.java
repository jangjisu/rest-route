package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightAirlineEntity;
import com.restroute.flight.repository.FlightAirlineRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * code→항공사명·저비용 여부 조회를 위한 순수 인메모리 캐시. DB 왕복 없이 딜 응답 조립 시
 * 읽기 전용으로 쓰인다. 시작 시 1회, 동기화 갱신 직후 1회 {@link #refresh()}로 다시
 * 채워진다.
 */
@Component
@RequiredArgsConstructor
public class FlightAirlineNameCache {

    private final FlightAirlineRepository flightAirlineRepository;

    private volatile Map<String, String> nameByCode = Map.of();
    private volatile Map<String, Boolean> isLowCostByCode = Map.of();

    public String findName(String code) {
        return nameByCode.get(code);
    }

    /** 코드가 없거나 소스(Travelpayouts)에 없었던 항공사는 저비용 여부를 알 수 없다는 뜻으로 false다. */
    public boolean isLowCost(String code) {
        return isLowCostByCode.getOrDefault(code, false);
    }

    public void refresh() {
        List<FlightAirlineEntity> airlines = flightAirlineRepository.findAll();
        nameByCode = airlines.stream()
                .collect(Collectors.toUnmodifiableMap(
                        FlightAirlineEntity::getCode, FlightAirlineNameCache::displayName));
        isLowCostByCode = airlines.stream()
                .collect(Collectors.toUnmodifiableMap(FlightAirlineEntity::getCode, FlightAirlineEntity::isLowCost));
    }

    private static String displayName(FlightAirlineEntity entity) {
        return entity.getKorName() != null ? entity.getKorName() : entity.getEngName();
    }
}
