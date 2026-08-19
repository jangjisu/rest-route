package com.restroute.flight.service.util;

import com.restroute.flight.controller.dto.FlightRegion;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.CollectionUtils;

/**
 * sector(지역권) 하나가 실제로 어떤 국가들을 가리키는지의 고정 매핑.
 *
 * <p>Travelpayouts grouped_prices는 destination에 국가 코드를 넣을 수 있어서, sector 검색은
 * "도시 하나하나" 대신 "이 sector에 속한 국가 하나하나"에 대해 한 번씩 조회한다 — 도시 단위로
 * 하면 나라마다 어떤 도시가 잘 알려져 있는지 계속 골라줘야 하는데, 국가 단위로 하면 그럴 필요가
 * 없어서 훨씬 단순하다.
 */
final class FlightSectorCountries {

    private static final Map<FlightRegion, List<String>> COUNTRIES_BY_REGION = Map.of(
            FlightRegion.JAPAN, List.of("JP"),
            FlightRegion.SOUTHEAST_ASIA, List.of("TH", "VN"),
            FlightRegion.GREATER_CHINA, List.of("CN", "TW", "HK", "MO"),
            FlightRegion.GUAM_SAIPAN, List.of("GU", "MP"));

    private FlightSectorCountries() {}

    /** 요청에 담긴 sector 원본 값(복수 선택 가능)을 실제 조회할 국가 코드 목록으로 펼친다 — 중복은 한 번만 남는다. */
    static List<String> countriesOf(List<String> sector) {
        if (CollectionUtils.isEmpty(sector)) {
            return List.of();
        }
        Set<String> countries = new LinkedHashSet<>();
        for (String raw : sector) {
            countries.addAll(COUNTRIES_BY_REGION.get(FlightRegion.valueOf(raw)));
        }
        return List.copyOf(countries);
    }
}
