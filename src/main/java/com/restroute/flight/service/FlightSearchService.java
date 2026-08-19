package com.restroute.flight.service;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightDealSort;
import com.restroute.flight.controller.dto.FlightSearchMode;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 항공권 실 연동 검색 진입점. 세션/페이지네이션·정렬은 여기서 공통으로 처리하고, 딜을 실제로
 * 어떻게 가져올지만 {@link #fetchDeals}로 뽑아뒀다 — {@link FlightSearchMockService}가 이
 * 메서드 하나만 오버라이드해서 고정 데이터를 돌려주고, 나머지(세션 생성/조회, 정렬)는 전부
 * 여기 구현을 그대로 물려 쓴다.
 *
 * <p>{@code totalSize}는 이 클래스 자체엔 의미가 없다(항상 null로 흐른다) — 모킹이 "총 몇 건
 * 생성할지"를 정할 때만 쓰는 값이라, {@link FlightSearchMockService}가 오버라이드한
 * {@link #fetchDeals}에서만 실제로 읽는다. {@link FlightDealSessionStore}는 이 값을 세션
 * 구분 키로만 쓴다.
 *
 * <p>cursor가 없는 첫 요청이면 {@link FlightDealSessionStore#create}로 조회+저장하고,
 * cursor가 있으면 {@link FlightDealSessionStore#find}로 이어서 가져온다 — 세션을 못
 * 찾으면(만료·조건 불일치 포함) {@link FlightDealNotFoundException}이 그대로 전파된다.
 * 페이지 계산·세션 토큰 같은 세부사항은 전부 {@link FlightDealSessionStore} 책임이다.
 *
 * <p>정렬은 세션 생성 시점(첫 요청)에 한 번만 적용된다 — 이후 cursor로 이어지는 페이지는
 * 이미 정렬된 세션 내부 리스트를 그대로 슬라이스하므로 페이지마다 다시 정렬할 필요가 없다.
 *
 * <p>{@link #fetchDeals}의 기본(= 실 연동) 구현은 {@link FlightSearchRequestDto#parsedSearchMode()}로
 * 갈린다 — RANGE는 {@link FlightRangeSearchPlanner}가 세운 계획대로 {@link FlightRangeSearchExecutor}가,
 * FIXED는 {@link FlightFixedSearchExecutor}가 각각 Travelpayouts를 호출한다. 이후
 * 매핑({@link FlightRangeSearchResponseMapper}) → 필터({@link FlightDealPostFilter}) →
 * 공휴일 배지 채우기({@link FlightDealHolidayEnricher}) → 전체 최저가 표시({@link
 * FlightDealResponses#markLowestInRange}) 순서로 조립한다.
 */
@Primary
@Service
@RequiredArgsConstructor
public class FlightSearchService {

    private final FlightDealSessionStore sessionStore;
    private final FlightRangeSearchExecutor rangeExecutor;
    private final FlightFixedSearchExecutor fixedExecutor;
    private final FlightRangeSearchResponseMapper responseMapper;
    private final FlightDealPostFilter postFilter;
    private final FlightDealHolidayEnricher holidayEnricher;

    public FlightDealSearchResponse search(FlightSearchRequestDto request) {
        return search(request, null);
    }

    FlightDealSearchResponse search(FlightSearchRequestDto request, Integer totalSize) {
        if (request.isFirstRequest()) {
            return sessionStore.create(
                    request, totalSize, request.boundedLimit(), token -> fetch(request, totalSize, token));
        }
        return sessionStore.find(request, totalSize, request.cursor(), request.boundedLimit());
    }

    private List<FlightDealResponse> fetch(FlightSearchRequestDto request, Integer totalSize, String token) {
        return sorted(fetchDeals(request, totalSize, token), request.parsedSort());
    }

    /** 딜 목록을 실제로 가져온다. {@link FlightSearchMockService}가 이 메서드만 오버라이드한다. */
    protected List<FlightDealResponse> fetchDeals(FlightSearchRequestDto request, Integer totalSize, String token) {
        List<TravelpayoutsPriceItem> rawItems = request.parsedSearchMode() == FlightSearchMode.RANGE
                ? rangeExecutor.execute(
                        request.origin(),
                        FlightRangeSearchPlanner.plan(request),
                        request.parsedDateFrom(),
                        request.parsedDateTo())
                : fixedExecutor.execute(request);

        List<FlightDealResponse> mapped = responseMapper.mapAll(rawItems, token);
        List<FlightDealResponse> filtered = postFilter.apply(mapped, request);
        List<FlightDealResponse> withHolidays = holidayEnricher.enrich(filtered);
        return FlightDealResponses.markLowestInRange(withHolidays);
    }

    static List<FlightDealResponse> sorted(List<FlightDealResponse> items, FlightDealSort sort) {
        return items.stream().sorted(comparatorFor(sort)).toList();
    }

    private static Comparator<FlightDealResponse> comparatorFor(FlightDealSort sort) {
        return switch (sort) {
            case PRICE -> Comparator.comparingInt(deal -> deal.price().amount());
            case DATE ->
                Comparator.comparing(
                        deal -> OffsetDateTime.parse(deal.departure().departAt()));
        };
    }
}
