package com.restroute.flight.cache;

import com.restroute.flight.domain.FlightAirlineEntity;
import com.restroute.flight.repository.FlightAirlineRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** code→항공사명·저비용 여부 조회 캐시 — 이름 조회의 공통 동작은 {@link ReferenceDataNameCache}. */
@Component
public class FlightAirlineNameCache extends ReferenceDataNameCache<FlightAirlineEntity> {

    private volatile Map<String, Boolean> isLowCostByCode = Map.of();

    public FlightAirlineNameCache(FlightAirlineRepository flightAirlineRepository) {
        super(
                flightAirlineRepository::findAll,
                FlightAirlineEntity::getCode,
                FlightAirlineEntity::getKorName,
                FlightAirlineEntity::getEngName);
    }

    /** 코드가 없거나 소스(Travelpayouts)에 없었던 항공사는 저비용 여부를 알 수 없다는 뜻으로 false다. */
    public boolean isLowCost(String code) {
        return isLowCostByCode.getOrDefault(code, false);
    }

    @Override
    protected void afterRefresh(List<FlightAirlineEntity> all) {
        isLowCostByCode = all.stream()
                .collect(Collectors.toUnmodifiableMap(FlightAirlineEntity::getCode, FlightAirlineEntity::isLowCost));
    }
}
