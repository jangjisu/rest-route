package com.restroute.service.route.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NearbyTrafficStatusTest {

    @Test
    @DisplayName("traffic_state 4는 원활이다")
    void from_mapsSmooth() {
        assertThat(NearbyTrafficStatus.from(4)).contains(NearbyTrafficStatus.SMOOTH);
        assertThat(NearbyTrafficStatus.SMOOTH.key()).isEqualTo("smooth");
        assertThat(NearbyTrafficStatus.SMOOTH.label()).isEqualTo("원활");
    }

    @Test
    @DisplayName("traffic_state 3은 서행이다")
    void from_mapsSlow() {
        assertThat(NearbyTrafficStatus.from(3)).contains(NearbyTrafficStatus.SLOW);
        assertThat(NearbyTrafficStatus.SLOW.key()).isEqualTo("slow");
        assertThat(NearbyTrafficStatus.SLOW.label()).isEqualTo("서행");
    }

    @Test
    @DisplayName("traffic_state 1과 2는 정체로 통합된다")
    void from_mapsJam() {
        assertThat(NearbyTrafficStatus.from(1)).contains(NearbyTrafficStatus.JAM);
        assertThat(NearbyTrafficStatus.from(2)).contains(NearbyTrafficStatus.JAM);
        assertThat(NearbyTrafficStatus.JAM.key()).isEqualTo("jam");
        assertThat(NearbyTrafficStatus.JAM.label()).isEqualTo("정체");
    }

    @Test
    @DisplayName("traffic_state 6은 사고다")
    void from_mapsAccident() {
        assertThat(NearbyTrafficStatus.from(6)).contains(NearbyTrafficStatus.ACCIDENT);
        assertThat(NearbyTrafficStatus.ACCIDENT.key()).isEqualTo("accident");
        assertThat(NearbyTrafficStatus.ACCIDENT.label()).isEqualTo("사고");
    }

    @Test
    @DisplayName("null, 0, 알 수 없는 값은 빈 값이다")
    void from_returnsEmptyForUnknownOrMissing() {
        assertThat(NearbyTrafficStatus.from(null)).isEqualTo(Optional.empty());
        assertThat(NearbyTrafficStatus.from(0)).isEqualTo(Optional.empty());
        assertThat(NearbyTrafficStatus.from(99)).isEqualTo(Optional.empty());
    }
}
