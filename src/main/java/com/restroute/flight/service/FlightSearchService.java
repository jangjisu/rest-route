package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightDealSort;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 항공권 검색 진입점. {@code totalSize}가 있으면 모킹 데이터를, 없으면(null) 실제
 * Travelpayouts 연동을 쓴다(실제 연동은 아직 미구현).
 *
 * <p>cursor가 없는 첫 요청이면 {@link FlightDealSessionStore#create}로 조회+저장하고,
 * cursor가 있으면 {@link FlightDealSessionStore#find}로 이어서 가져온다 — 세션을 못
 * 찾으면(만료·조건 불일치 포함) {@link FlightDealNotFoundException}이 그대로 전파된다.
 * 페이지 계산·세션 토큰 같은 세부사항은 전부 {@link FlightDealSessionStore} 책임이다.
 *
 * <p>정렬은 세션 생성 시점(첫 요청)에 한 번만 적용된다 — 이후 cursor로 이어지는 페이지는
 * 이미 정렬된 세션 내부 리스트를 그대로 슬라이스하므로 페이지마다 다시 정렬할 필요가 없다.
 */
@Service
@RequiredArgsConstructor
public class FlightSearchService {

    private final FlightDealSessionStore sessionStore;

    /** SessionStore를 외부에 노출하지 않고도 실제 객체로 조립해 테스트/수동 배선할 때 쓰는 편의 생성자. */
    public FlightSearchService() {
        this(FlightDealSessionStore.create());
    }

    /** 실제 연동 전용 진입점. */
    public FlightDealSearchResponse search(FlightSearchRequestDto request) {
        return search(request, null);
    }

    /** 모킹 전용 진입점 — totalSize가 이번 검색에서 총 몇 건 조회할지를 뜻한다. */
    public FlightDealSearchResponse search(FlightSearchRequestDto request, Integer totalSize) {
        if (request.isFirstRequest()) {
            return sessionStore.create(
                    request, totalSize, request.boundedSize(), token -> fetch(request, totalSize, token));
        }
        return sessionStore.find(request, totalSize, request.cursor(), request.boundedSize());
    }

    private List<FlightDealResponse> fetch(FlightSearchRequestDto request, Integer totalSize, String token) {
        boolean isMocking = totalSize != null;
        if (isMocking) {
            return sorted(FlightSearchMockFixture.generateAll(request, token, totalSize), request.parsedSort());
        }
        throw new UnsupportedOperationException("실제 Travelpayouts 연동은 아직 없습니다.");
    }

    private static List<FlightDealResponse> sorted(List<FlightDealResponse> items, FlightDealSort sort) {
        return items.stream().sorted(comparatorFor(sort)).toList();
    }

    private static Comparator<FlightDealResponse> comparatorFor(FlightDealSort sort) {
        return switch (sort) {
            case PRICE -> Comparator.comparingInt(deal -> deal.price().amount());
            case DATE -> Comparator.comparing(deal -> OffsetDateTime.parse(deal.departure().departureFrom()));
        };
    }
}
