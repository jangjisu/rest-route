package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 검색 요청 하나가 실제로 몇 개의 destination 단위를 조회해야 하는지 정한다. RANGE/FIXED 둘 다
 * 이 판단이 똑같다 — nights·개월 팬아웃 여부와 무관하게, "이 요청이 어떤 destination
 * 파라미터들로 나뉘는가"는 destination/sector 필드만으로 정해지기 때문이다.
 *
 * <p>destination을 직접 지정했으면 그거 하나, sector면 그 sector들의 국가 목록, 둘 다 없으면
 * 빈 목록 — 빈 목록은 destination 파라미터 자체를 생략하라는 신호다(grouped_prices가 알아서
 * 여러 목적지를 섞어준다).
 */
final class FlightSearchDestinations {

    private FlightSearchDestinations() {}

    static List<String> resolve(FlightSearchRequestDto request) {
        if (StringUtils.hasText(request.destination())) {
            return List.of(request.destination());
        }
        return FlightSectorCountries.countriesOf(request.sector());
    }
}
