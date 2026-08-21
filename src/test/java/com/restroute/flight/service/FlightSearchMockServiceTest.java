package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchMockServiceTest {

    private static final String VALID_ORIGIN = "ICN";
    private static final String VALID_DATE_FROM = LocalDate.now().plusDays(10).toString();
    private static final String VALID_DATE_TO = LocalDate.now().plusDays(41).toString();
    private static final int MOCK_TOTAL_SIZE = 77;

    private final FlightSearchMockService service = new FlightSearchMockService(new FlightDealSessionStore());

    @Test
    @DisplayName("cursor가 없으면(첫 요청) 조회해서 세션에 저장하고 첫 페이지를 반환한다")
    void search_fetchesAndSavesOnFirstRequest() {
        FlightDealSearchResponse response = service.search(request(null, "3", "3"));

        assertThat(response.items()).hasSize(3);
        assertThat(response.meta().totalCount()).isEqualTo(MOCK_TOTAL_SIZE);
        assertThat(response.meta().hasNext()).isTrue();
    }

    @Test
    @DisplayName("같은 조건으로 cursor를 이어주면 정렬된 순서 그대로, 중복 없이 이어서 반환한다")
    void search_continuesFromSessionStoreWhenCursorMatches() {
        FlightDealSearchResponse first = service.search(request(null, "3", "3"));
        FlightDealSearchResponse second = service.search(request(first.meta().nextCursor(), "3", "3"));

        List<Integer> combinedPrices = Stream.concat(first.items().stream(), second.items().stream())
                .map(item -> item.price().amount())
                .toList();
        assertThat(combinedPrices).isSorted();
        assertThat(second.items())
                .extracting(FlightDealResponse::id)
                .doesNotContainAnyElementsOf(
                        first.items().stream().map(FlightDealResponse::id).toList());
    }

    @Test
    @DisplayName("cursor가 있는데 세션을 못 찾으면(형식 이상/만료/조건 불일치 포함) 잘못된 요청으로 실패한다")
    void search_failsWhenCursorCannotBeResolved() {
        assertThatThrownBy(() -> service.search(request("not-a-real-cursor", "3", null)))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    @Test
    @DisplayName("검색 조건이 달라진 채로 cursor를 재사용하면 실패한다")
    void search_failsWhenSearchConditionsChange() {
        FlightDealSearchResponse first = service.search(request(null, "3", "3"));

        assertThatThrownBy(() -> service.search(request(first.meta().nextCursor(), "4", "3")))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    @Test
    @DisplayName("sort가 없으면(기본 PRICE) 가격 오름차순으로 정렬된 결과를 반환한다")
    void search_sortsByPriceAscending_whenSortIsDefault() {
        FlightDealSearchResponse response = service.search(requestWithSort(null, "3", "30", null));

        List<Integer> prices =
                response.items().stream().map(item -> item.price().amount()).toList();
        assertThat(prices).isSorted();
    }

    @Test
    @DisplayName("sort=DATE면 출발일 오름차순으로 정렬된 결과를 반환한다")
    void search_sortsByDepartureDateAscending_whenSortIsDate() {
        FlightDealSearchResponse response = service.search(requestWithSort(null, "3", "30", "DATE"));

        List<String> departures = response.items().stream()
                .map(item -> item.departure().departAt())
                .toList();
        assertThat(departures).isSorted();
    }

    private static FlightSearchRequestDto request(String cursor, String nights, String limit) {
        return requestWithSort(cursor, nights, limit, null);
    }

    /** includeWeekend=true로 고정한다 — MOCK_TOTAL_SIZE(77)개가 필터 없이 그대로 저장/순회되는
     * 걸 전제로 하는 테스트라서, 주말 필터링과 뒤섞이지 않게 한다. */
    private static FlightSearchRequestDto requestWithSort(String cursor, String nights, String limit, String sort) {
        return new FlightSearchRequestDto(
                VALID_ORIGIN,
                "range",
                VALID_DATE_FROM,
                VALID_DATE_TO,
                null,
                List.of(nights),
                null,
                "true",
                null,
                null,
                sort,
                cursor,
                limit,
                null,
                null);
    }
}
