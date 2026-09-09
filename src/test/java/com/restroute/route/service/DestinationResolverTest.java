package com.restroute.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DestinationResolverTest {

    private final DestinationResolver resolver = new DestinationResolver();

    @Test
    @DisplayName("좌표가 함께 오면 그 좌표를 그대로 목적지로 삼는다")
    void coordinatesGiven_areUsedAsIs() {
        Destination destination = resolver.resolve(35.1148, 129.0403, "부산역");

        assertThat(destination.latitude()).isEqualTo(35.1148);
        assertThat(destination.longitude()).isEqualTo(129.0403);
        assertThat(destination.name()).isEqualTo("부산역");
    }

    @Test
    @DisplayName("좌표만 있고 이름이 없으면 기본 표시명으로 대체한다")
    void coordinatesWithoutName_fallBackToDefaultLabel() {
        assertThat(resolver.resolve(35.1148, 129.0403, null).name()).isEqualTo("목적지");
        assertThat(resolver.resolve(35.1148, 129.0403, "  ").name()).isEqualTo("목적지");
    }

    @Test
    @DisplayName("좌표 없이 온 이름은 서버가 아는 목적지 목록에서 좌표를 찾는다")
    void knownDestinationName_resolvesToItsCoordinates() {
        Destination destination = resolver.resolve(null, null, "대전역");

        assertThat(destination.name()).isEqualTo("대전역");
        assertThat(destination.latitude()).isEqualTo(36.331331);
        assertThat(destination.longitude()).isEqualTo(127.433019);
    }

    /** 위도·경도 중 하나만 와도 좌표로 인정하지 않는다 — 반쪽 좌표로 길찾기를 부를 수는 없다. */
    @Test
    @DisplayName("좌표가 한쪽만 오면 이름으로 해석한다")
    void partialCoordinates_fallBackToNameLookup() {
        assertThat(resolver.resolve(35.1148, null, "부산역").longitude()).isEqualTo(129.04137);
        assertThat(resolver.resolve(null, 129.0403, "부산역").latitude()).isEqualTo(35.11520);
    }

    @Test
    @DisplayName("서버가 좌표를 모르는 이름이면 NotFound다")
    void unknownDestinationName_throwsNotFound() {
        assertThatThrownBy(() -> resolver.resolve(null, null, "없는곳"))
                .isInstanceOf(RouteRestStopNotFoundException.class)
                .hasMessageContaining("없는곳");
    }

    @Test
    @DisplayName("이름이 아예 없으면 NotFound다")
    void noDestinationAtAll_throwsNotFound() {
        assertThatThrownBy(() -> resolver.resolve(null, null, null)).isInstanceOf(RouteRestStopNotFoundException.class);
    }
}
