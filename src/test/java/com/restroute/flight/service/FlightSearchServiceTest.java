package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchServiceTest {

    private static final String VALID_ORIGIN = "ICN";
    private static final String VALID_DATE_FROM = "2099-01-10";
    private static final String VALID_DATE_TO = "2099-02-10";

    private final FlightSearchService service = new FlightSearchService(new FlightDealSessionStore());

    @Test
    @DisplayName("cursor가 없으면(첫 요청) 조회해서 세션에 저장하고 첫 페이지를 반환한다")
    void search_fetchesAndSavesOnFirstRequest() {
        FlightDealSearchResponse response = service.search(request(null, "3", "3"), 5);

        assertThat(response.items()).hasSize(3);
        assertThat(response.meta().totalCount()).isEqualTo(5);
        assertThat(response.meta().hasNext()).isTrue();
    }

    @Test
    @DisplayName("같은 조건으로 cursor를 이어주면 세션스토어에서 찾아 이어서 반환한다")
    void search_continuesFromSessionStoreWhenCursorMatches() {
        FlightDealSearchResponse first = service.search(request(null, "3", "3"), 10);
        FlightDealSearchResponse second = service.search(request(first.meta().nextCursor(), "3", "3"), 10);

        String firstToken = first.items().get(0).id().split("_")[0];
        assertThat(second.items().get(0).id()).isEqualTo(firstToken + "_0004");
    }

    @Test
    @DisplayName("cursor가 있는데 세션을 못 찾으면(형식 이상/만료/조건 불일치 포함) 잘못된 요청으로 실패한다")
    void search_failsWhenCursorCannotBeResolved() {
        assertThatThrownBy(() -> service.search(request("not-a-real-cursor", "3", null), 5))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    @Test
    @DisplayName("검색 조건이 달라진 채로 cursor를 재사용하면 실패한다")
    void search_failsWhenSearchConditionsChange() {
        FlightDealSearchResponse first = service.search(request(null, "3", "3"), 10);

        assertThatThrownBy(() -> service.search(request(first.meta().nextCursor(), "4", "3"), 10))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    @Test
    @DisplayName("totalSize가 null이면(실제 연동) 아직 미구현이라 예외를 던진다")
    void search_throwsForRealModeNotYetImplemented() {
        assertThatThrownBy(() -> service.search(request(null, "3", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("sort가 없으면(기본 PRICE) 가격 오름차순으로 정렬된 결과를 반환한다")
    void search_sortsByPriceAscending_whenSortIsDefault() {
        FlightDealSearchResponse response = service.search(requestWithSort(null, "3", "30", null), 30);

        List<Integer> prices =
                response.items().stream().map(item -> item.price().amount()).toList();
        assertThat(prices).isSorted();
    }

    @Test
    @DisplayName("sort=DATE면 출발일 오름차순으로 정렬된 결과를 반환한다")
    void search_sortsByDepartureDateAscending_whenSortIsDate() {
        FlightDealSearchResponse response = service.search(requestWithSort(null, "3", "30", "DATE"), 30);

        List<String> departures = response.items().stream()
                .map(item -> item.departure().departureFrom())
                .toList();
        assertThat(departures).isSorted();
    }

    private static FlightSearchRequestDto request(String cursor, String nights, String limit) {
        return requestWithSort(cursor, nights, limit, null);
    }

    private static FlightSearchRequestDto requestWithSort(String cursor, String nights, String limit, String sort) {
        return new FlightSearchRequestDto(
                VALID_ORIGIN,
                "range",
                VALID_DATE_FROM,
                VALID_DATE_TO,
                null,
                List.of(nights),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sort,
                cursor,
                limit,
                null,
                null);
    }
}
