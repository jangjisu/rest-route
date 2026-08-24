package com.restroute.route.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteRestStopResponseTest {

    @Test
    @DisplayName("경로 결과 응답은 전국 평균가 요약 필드를 노출하지 않는다")
    void routeResponse_doesNotExposeNationalOilPriceSummary() {
        RouteRestStopResponse response = RouteRestStopResponse.of(
                RouteRestStopResponse.Destination.of("부산역", 35.0, 129.0),
                List.of(RouteRestStopResponse.RouteOption.of(
                        0, RouteRestStopResponse.RouteSummary.of(100L, 200L, 0L, List.of()), List.of())));

        assertThat(Arrays.stream(RouteRestStopResponse.class.getRecordComponents())
                        .map(component -> component.getName()))
                .doesNotContain("nationalOilPriceSummary");
        assertThat(response.routes())
                .singleElement()
                .satisfies(route -> assertThat(route.restStops()).isEmpty());
    }

    @Test
    @DisplayName("경로 휴게소의 목록 이미지 URL은 후속 응답 변환에서도 보존한다")
    void routeRestStopItem_preservesListImageUrlAcrossResponseTransformations() {
        RouteRestStopResponse.RouteRestStopItem item = RouteRestStopResponse.RouteRestStopItem.of(
                        "A", "A휴게소", "경부선", 37.0, 127.0, 12L)
                .withListImageUrl("/api/rest-stops/A/images/list")
                .withEvCharger(true)
                .withDirectionAlternative(true)
                .withComparison(RouteRestStopResponse.ComparisonSummary.empty(), List.of());

        assertThat(item.listImageUrl()).isEqualTo("/api/rest-stops/A/images/list");
    }

    @Test
    @DisplayName("테마/이벤트 배지 여부를 다른 필드를 보존하며 갱신한다")
    void routeRestStopItem_withThemeAndWithEvent_preservesOtherFields() {
        RouteRestStopResponse.RouteRestStopItem item = RouteRestStopResponse.RouteRestStopItem.of(
                        "A", "A휴게소", "경부선", 37.0, 127.0, 12L)
                .withListImageUrl("/api/rest-stops/A/images/list")
                .withEvCharger(true)
                .withTheme(true)
                .withEvent(true);

        assertThat(item.hasTheme()).isTrue();
        assertThat(item.hasEvent()).isTrue();
        assertThat(item.hasEvCharger()).isTrue();
        assertThat(item.listImageUrl()).isEqualTo("/api/rest-stops/A/images/list");
    }

    @Test
    @DisplayName("테마/이벤트 배지 기본값은 false다")
    void routeRestStopItem_defaultsThemeAndEventToFalse() {
        RouteRestStopResponse.RouteRestStopItem item =
                RouteRestStopResponse.RouteRestStopItem.of("A", "A휴게소", "경부선", 37.0, 127.0, 12L);

        assertThat(item.hasTheme()).isFalse();
        assertThat(item.hasEvent()).isFalse();
    }
}
