package com.restroute.service.route.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.client.response.KakaoDirectionsResponse.Road;
import com.restroute.client.response.KakaoDirectionsResponse.Route;
import com.restroute.client.response.KakaoDirectionsResponse.Section;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoutePolylineTest {

    private static Route route(List<Double> vertexes) {
        return new Route(0, null, List.of(new Section(List.of(new Road(vertexes)))));
    }

    @Test
    @DisplayName("fromRoute는 [경도,위도,...] 평탄 배열을 좌표쌍으로 펼친다")
    void fromRoute_flattensVertexes() {
        RoutePolyline polyline = RoutePolyline.fromRoute(route(List.of(127.0, 37.0, 127.1, 37.1)));

        assertThat(polyline.size()).isEqualTo(2);
        assertThat(polyline.coordinates().get(0).longitude()).isEqualTo(127.0);
        assertThat(polyline.coordinates().get(0).latitude()).isEqualTo(37.0);
        assertThat(polyline.coordinates().get(1).longitude()).isEqualTo(127.1);
    }

    @Test
    @DisplayName("fromRoute는 null route/section/road/vertex를 안전하게 건너뛴다")
    void fromRoute_handlesNulls() {
        assertThat(RoutePolyline.fromRoute(null).isEmpty()).isTrue();
        assertThat(RoutePolyline.fromRoute(new Route(0, null, null)).isEmpty()).isTrue();

        List<Section> sections = new ArrayList<>();
        sections.add(null);
        sections.add(new Section(null));
        sections.add(new Section(sectionWithNullRoad()));
        assertThat(RoutePolyline.fromRoute(new Route(0, null, sections)).isEmpty())
                .isTrue();

        List<Double> withNull = Arrays.asList(null, 37.0, 127.0, null, 127.1, 37.1);
        assertThat(RoutePolyline.fromRoute(route(withNull)).size()).isEqualTo(1);
    }

    private static List<Road> sectionWithNullRoad() {
        List<Road> roads = new ArrayList<>();
        roads.add(null);
        roads.add(new Road(null));
        return roads;
    }

    @Test
    @DisplayName("downsample은 정점 수를 maxPoints 이하로 줄이고 시작/끝을 보존한다")
    void downsample_reducesPoints() {
        List<Double> vertexes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            vertexes.add(127.0 + i * 0.01);
            vertexes.add(37.0 + i * 0.01);
        }
        RoutePolyline polyline = RoutePolyline.fromRoute(route(vertexes));

        RoutePolyline sampled = polyline.downsample(10);

        assertThat(sampled.size()).isEqualTo(10);
        assertThat(sampled.coordinates().get(0).longitude()).isEqualTo(127.0);
        assertThat(sampled.coordinates().get(9).longitude())
                .isEqualTo(polyline.coordinates().get(49).longitude());
    }

    @Test
    @DisplayName("downsample은 maxPoints보다 작거나 2 미만이면 그대로 반환한다")
    void downsample_noop() {
        RoutePolyline polyline = RoutePolyline.fromRoute(route(List.of(127.0, 37.0, 127.1, 37.1)));

        assertThat(polyline.downsample(10).size()).isEqualTo(2);
        assertThat(polyline.downsample(1).size()).isEqualTo(2);
    }

    @Test
    @DisplayName("fromRoute는 각 좌표에 그 좌표가 속한 도로의 traffic_state를 채운다")
    void fromRoute_fillsTrafficStatePerRoad() {
        Road jamRoad = new Road("테헤란로", 24L, 9L, 9, 1, List.of(127.0, 37.0, 127.1, 37.1));
        Road smoothRoad = new Road("경부선", 500L, 20L, 90, 4, List.of(128.0, 38.0));
        Route route = new Route(0, null, List.of(new Section(List.of(jamRoad, smoothRoad))));

        RoutePolyline polyline = RoutePolyline.fromRoute(route);

        assertThat(polyline.coordinates().get(0).trafficState()).isEqualTo(1);
        assertThat(polyline.coordinates().get(1).trafficState()).isEqualTo(1);
        assertThat(polyline.coordinates().get(2).trafficState()).isEqualTo(4);
    }

    @Test
    @DisplayName("도로에 traffic_state가 없으면 좌표의 trafficState도 null이다")
    void fromRoute_leavesTrafficStateNullWhenRoadHasNone() {
        RoutePolyline polyline = RoutePolyline.fromRoute(route(List.of(127.0, 37.0)));

        assertThat(polyline.coordinates().get(0).trafficState()).isNull();
    }

    @Test
    @DisplayName("nearest는 최단거리(m)와 가장 가까운 정점 인덱스를 반환한다")
    void nearest_returnsClosest() {
        RoutePolyline polyline = RoutePolyline.fromRoute(route(List.of(127.0, 37.0, 128.0, 38.0)));

        RoutePolyline.Nearest near = polyline.nearest(37.0001, 127.0001);
        assertThat(near.index()).isEqualTo(0);
        assertThat(near.distanceMeters()).isLessThan(50);

        RoutePolyline.Nearest far = polyline.nearest(38.0, 128.0);
        assertThat(far.index()).isEqualTo(1);
        assertThat(far.distanceMeters()).isLessThan(1);
    }
}
