package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.util.Collections;
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

    /**
     * destination을 직접 지정한 게 아니라 sector로 여러 국가가 잡힌 경우인지 알려준다 — direct
     * destination은 사용자가 정확히 그 하나만 원한 것이므로 "전체" 조회를 덧붙이면 안 되고,
     * sector는 지역 단위 선택이라 "전체"를 함께 보여주는 게 의미가 있다.
     */
    static boolean isSectorBased(FlightSearchRequestDto request) {
        return !StringUtils.hasText(request.destination());
    }

    /**
     * {@link #resolve}가 빈 목록(생략)을 돌려주면 실행기 입장에서는 "destination 파라미터
     * 없이 딱 한 번만 호출하라"는 뜻이다 — 그 신호를 {@code null} 하나짜리 목록으로 통일해서
     * RANGE/FIXED 실행기가 각자 다시 구현하지 않고 여기서 한 번만 처리한다.
     */
    static List<String> paddedForCalls(List<String> destinations) {
        return destinations.isEmpty() ? Collections.singletonList(null) : destinations;
    }
}
