package com.restroute.flight.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightDealSessionStoreTest {

    private final FlightDealSessionStore store = new FlightDealSessionStore();

    @Test
    @DisplayName("cursor가 없으면 새 세션을 만들고 1번 항목부터 반환한다")
    void page_createsNewSessionWhenCursorIsNull() {
        FlightDealSearchResponse response = store.page(request("3"), 5, null, 5);

        assertThat(response.items()).hasSize(5);
        assertThat(response.items().get(0).id()).matches("^[A-Za-z0-9]{4}_0001$");
        assertThat(response.meta().totalCount()).isEqualTo(5);
        assertThat(response.meta().hasNext()).isFalse();
    }

    @Test
    @DisplayName("같은 조건으로 cursor를 이어주면 같은 세션에서 이어서 반환한다")
    void page_reusesSessionWhenConditionsMatch() {
        FlightDealSearchResponse first = store.page(request("3"), 10, null, 4);
        String firstToken = first.items().get(0).id().split("_")[0];

        FlightDealSearchResponse second =
                store.page(request("3"), 10, first.meta().nextCursor(), 4);

        assertThat(second.items().get(0).id()).isEqualTo(firstToken + "_0005");
        assertThat(second.meta().totalCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("검색 조건이 달라지면 에러 없이 새 세션 1번 항목부터 다시 시작한다")
    void page_startsNewSessionWhenConditionsDiffer() {
        FlightDealSearchResponse first = store.page(request("3"), 10, null, 4);
        String cursorFromDifferentConditions = first.meta().nextCursor();

        FlightDealSearchResponse second = store.page(request("4"), 10, cursorFromDifferentConditions, 4);

        assertThat(second.items().get(0).id()).matches("^[A-Za-z0-9]{4}_0001$");
        assertThat(second.items().get(0).nights()).isEqualTo(4);
    }

    @Test
    @DisplayName("세션 토큰을 뽑을 수 없는(구분자가 없는) cursor는 에러 없이 새 세션으로 처리된다")
    void page_startsNewSessionWhenCursorHasNoSeparator() {
        FlightDealSearchResponse response = store.page(request("3"), 5, "not-a-real-cursor", 5);

        assertThat(response.items().get(0).id()).matches("^[A-Za-z0-9]{4}_0001$");
    }

    @Test
    @DisplayName("세션이 만료되면 에러 없이 새 세션 1번 항목부터 다시 시작한다")
    void page_startsNewSessionWhenExpired() throws InterruptedException {
        FlightDealSessionStore shortLivedStore = new FlightDealSessionStore(Duration.ofMillis(5));
        FlightDealSearchResponse first = shortLivedStore.page(request("3"), 10, null, 4);
        String expiredCursor = first.meta().nextCursor();

        Thread.sleep(50);
        FlightDealSearchResponse second = shortLivedStore.page(request("3"), 10, expiredCursor, 4);

        assertThat(second.items().get(0).id()).matches("^[A-Za-z0-9]{4}_0001$");
    }

    @Test
    @DisplayName("조건은 일치하는 유효한 세션인데 그 안에 없는 id면 DEAL_NOT_FOUND를 던진다")
    void page_throwsDealNotFoundForUnknownIdWithinValidSession() {
        FlightDealSearchResponse first = store.page(request("3"), 5, null, 1);
        String token = first.items().get(0).id().split("_")[0];
        String bogusCursor = token + "_9999";

        assertThatThrownBy(() -> store.page(request("3"), 5, bogusCursor, 1))
                .isInstanceOf(FlightDealNotFoundException.class);
    }

    private static FlightSearchRequestValidator.ValidatedRequest request(String nights) {
        return FlightSearchRequestValidator.validate(
                "ICN", null, "2099-01-10", "2099-02-10", List.of(nights), null, null, null);
    }
}
