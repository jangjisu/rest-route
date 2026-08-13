package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightDealSessionStoreTest {

    private final FlightDealSessionStore store = new FlightDealSessionStore();

    @Test
    @DisplayName("create는 조회 결과를 저장하고 첫 페이지를 반환한다")
    void create_savesAndReturnsFirstPage() {
        FlightDealSearchResponse response = create(store, request("3"), 5, 3);

        assertThat(response.items()).hasSize(3);
        assertThat(response.meta().totalCount()).isEqualTo(5);
        assertThat(response.meta().hasNext()).isTrue();
        assertThat(response.meta().nextCursor()).isNotNull();
    }

    @Test
    @DisplayName("create가 만든 cursor로 find를 부르면 같은 세션에서 이어서 반환한다")
    void find_continuesFromSessionCreatedByCreate() {
        FlightDealSearchResponse first = create(store, request("3"), 10, 3);

        FlightDealSearchResponse second =
                store.find(request("3"), 10, first.meta().nextCursor(), 3);

        String firstToken = first.items().get(0).id().split("_")[0];
        assertThat(second.items().get(0).id()).isEqualTo(firstToken + "_0004");
    }

    @Test
    @DisplayName("검색 조건이 다르면 find는 DEAL_NOT_FOUND로 실패한다")
    void find_throws_whenConditionsDiffer() {
        FlightDealSearchResponse first = create(store, request("3"), 5, 1);

        assertThatThrownBy(
                        () -> store.find(request("4"), 5, first.items().get(0).id(), 1))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    @Test
    @DisplayName("totalSize가 다르면 find는 DEAL_NOT_FOUND로 실패한다")
    void find_throws_whenTotalSizeDiffers() {
        FlightDealSearchResponse first = create(store, request("3"), 5, 1);

        assertThatThrownBy(
                        () -> store.find(request("3"), 10, first.items().get(0).id(), 1))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    @Test
    @DisplayName("세션 토큰을 뽑을 수 없는(구분자가 없는) cursor는 find가 DEAL_NOT_FOUND로 실패한다")
    void find_throws_whenCursorHasNoSeparator() {
        assertThatThrownBy(() -> store.find(request("3"), 5, "not-a-real-cursor", 5))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    @Test
    @DisplayName("세션 안에 실제로 없는 id면 find는 DEAL_NOT_FOUND로 실패한다")
    void find_throws_whenIdMissingWithinValidSession() {
        FlightDealSearchResponse first = create(store, request("3"), 5, 1);
        String token = first.items().get(0).id().split("_")[0];

        assertThatThrownBy(() -> store.find(request("3"), 5, token + "_9999", 1))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    @Test
    @DisplayName("세션이 만료되면 find는 DEAL_NOT_FOUND로 실패한다")
    void find_throws_whenExpired() throws InterruptedException {
        FlightDealSessionStore shortLivedStore = new FlightDealSessionStore(Duration.ofMillis(5));
        FlightDealSearchResponse first = create(shortLivedStore, request("3"), 5, 1);

        Thread.sleep(50);

        assertThatThrownBy(() -> shortLivedStore.find(
                        request("3"), 5, first.items().get(0).id(), 1))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    private static FlightDealSearchResponse create(
            FlightDealSessionStore sessionStore, FlightSearchRequestDto req, int totalSize, int size) {
        return sessionStore.create(
                req, totalSize, size, token -> FlightSearchMockFixture.generateAll(req, token, totalSize));
    }

    private static FlightSearchRequestDto request(String nights) {
        return new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of(nights), null, null, null, null, null, null);
    }
}
