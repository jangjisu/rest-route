package com.restroute.route.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PopularDestinationTest {

    @Test
    @DisplayName("표시명으로 찾으면 그 목적지가 나온다")
    void findByDisplayName_returnsMatchingDestination() {
        assertThat(PopularDestination.findByDisplayName("부산역")).contains(PopularDestination.BUSAN_STATION);
        assertThat(PopularDestination.findByDisplayName("광주송정역"))
                .contains(PopularDestination.GWANGJU_SONGJEONG_STATION);
    }

    @Test
    @DisplayName("목록에 없는 이름이면 비어 있다")
    void findByDisplayName_returnsEmptyForUnknownName() {
        assertThat(PopularDestination.findByDisplayName("없는곳")).isEmpty();
        assertThat(PopularDestination.findByDisplayName(null)).isEmpty();
    }

    /**
     * 좌표가 대한민국 안에 있는지만 본다 — 정확한 값은 여기서 지킬 수 없지만, 부호가 뒤집히거나
     * 위경도가 서로 바뀌는 실수는 이 범위에서 걸린다.
     */
    @Test
    @DisplayName("모든 목적지 좌표가 대한민국 범위 안에 있다")
    void allDestinations_haveCoordinatesWithinKorea() {
        assertThat(Arrays.asList(PopularDestination.values())).allSatisfy(destination -> {
            assertThat(destination.latitude()).isBetween(33.0, 39.0);
            assertThat(destination.longitude()).isBetween(124.0, 132.0);
        });
    }

    @Test
    @DisplayName("표시명이 서로 겹치지 않는다")
    void displayNames_areUnique() {
        assertThat(Arrays.stream(PopularDestination.values())
                        .map(PopularDestination::displayName)
                        .distinct()
                        .count())
                .isEqualTo(PopularDestination.values().length);
    }
}
