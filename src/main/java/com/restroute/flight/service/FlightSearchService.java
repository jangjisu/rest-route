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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 항공권 검색 진입점. {@code totalSize}가 있으면 모킹 데이터를, 없으면(null) 실제
 * Travelpayouts 연동을 쓴다.
 *
 * <p>cursor가 없는 첫 요청이면 {@link FlightDealSessionStore#create}로 조회+저장하고,
 * cursor가 있으면 {@link FlightDealSessionStore#find}로 이어서 가져온다 — 세션을 못
 * 찾으면(만료·조건 불일치 포함) {@link FlightDealNotFoundException}이 그대로 전파된다.
 * 페이지 계산·세션 토큰 같은 세부사항은 전부 {@link FlightDealSessionStore} 책임이다.
 *
 * <p>정렬은 세션 생성 시점(첫 요청)에 한 번만 적용된다 — 이후 cursor로 이어지는 페이지는
 * 이미 정렬된 세션 내부 리스트를 그대로 슬라이스하므로 페이지마다 다시 정렬할 필요가 없다.
 *
 * <p>실제 연동은 {@link FlightSearchRequestDto#parsedSearchMode()}로 갈린다 — RANGE는
 * {@link FlightRangeSearchPlanner}가 세운 계획대로 {@link FlightRangeSearchExecutor}가,
 * FIXED는 {@link FlightFixedSearchExecutor}가 각각 Travelpayouts를 호출한다. 이후
 * 매핑({@link FlightRangeSearchResponseMapper}) → 필터({@link FlightDealPostFilter}) →
 * 공휴일 배지 채우기({@link FlightDealHolidayEnricher}) → 전체 최저가 표시({@link
 * FlightDealResponses#markLowestInRange}) → 정렬 순서는 두 모드가 공통으로 거친다.
 */
@Service
public class FlightSearchService {

    private final FlightDealSessionStore sessionStore;
    private final FlightRangeSearchExecutor rangeExecutor;
    private final FlightFixedSearchExecutor fixedExecutor;
    private final FlightRangeSearchResponseMapper responseMapper;
    private final FlightDealPostFilter postFilter;
    private final FlightDealHolidayEnricher holidayEnricher;

    /**
     * 스프링이 실제로 쓰는 생성자. 생성자가 여러 개라 {@code @Autowired}로 명시하지 않으면
     * 스프링이 아래 편의 생성자(모킹 전용, 실 연동 의존성이 비어있음)를 대신 골라버릴 수 있다.
     */
    @Autowired
    public FlightSearchService(
            FlightDealSessionStore sessionStore,
            FlightRangeSearchExecutor rangeExecutor,
            FlightFixedSearchExecutor fixedExecutor,
            FlightRangeSearchResponseMapper responseMapper,
            FlightDealPostFilter postFilter,
            FlightDealHolidayEnricher holidayEnricher) {
        this.sessionStore = sessionStore;
        this.rangeExecutor = rangeExecutor;
        this.fixedExecutor = fixedExecutor;
        this.responseMapper = responseMapper;
        this.postFilter = postFilter;
        this.holidayEnricher = holidayEnricher;
    }

    /** SessionStore를 외부에 노출하지 않고도 실제 객체로 조립해 테스트/수동 배선할 때 쓰는 편의 생성자(모킹 전용). */
    public FlightSearchService() {
        this(FlightDealSessionStore.create());
    }

    /** 위와 동일하되 SessionStore를 직접 넘길 때 쓴다(모킹 전용 — 실제 연동 의존성은 비워둔다). */
    public FlightSearchService(FlightDealSessionStore sessionStore) {
        this(sessionStore, null, null, null, null, null);
    }

    /** 실제 연동 전용 진입점. */
    public FlightDealSearchResponse search(FlightSearchRequestDto request) {
        return search(request, null);
    }

    /** 모킹 전용 진입점 — totalSize가 이번 검색에서 총 몇 건 조회할지를 뜻한다. */
    public FlightDealSearchResponse search(FlightSearchRequestDto request, Integer totalSize) {
        if (request.isFirstRequest()) {
            return sessionStore.create(
                    request, totalSize, request.boundedLimit(), token -> fetch(request, totalSize, token));
        }
        return sessionStore.find(request, totalSize, request.cursor(), request.boundedLimit());
    }

    private List<FlightDealResponse> fetch(FlightSearchRequestDto request, Integer totalSize, String token) {
        boolean isMocking = totalSize != null;
        if (isMocking) {
            return sorted(FlightSearchMockFixture.generateAll(request, token, totalSize), request.parsedSort());
        }
        return sorted(fetchReal(request, token), request.parsedSort());
    }

    private List<FlightDealResponse> fetchReal(FlightSearchRequestDto request, String token) {
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

    private static List<FlightDealResponse> sorted(List<FlightDealResponse> items, FlightDealSort sort) {
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
