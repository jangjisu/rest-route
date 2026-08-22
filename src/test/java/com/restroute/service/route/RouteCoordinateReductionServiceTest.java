package com.restroute.service.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.client.response.KakaoDirectionsResponse.Road;
import com.restroute.client.response.KakaoDirectionsResponse.Route;
import com.restroute.client.response.KakaoDirectionsResponse.Section;
import com.restroute.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.service.route.dto.ResolvedRoute.RouteGeometry;
import com.restroute.service.route.dto.RoutePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RouteRestStopService.reduceCoordinates()가 위임하는 실제 좌표 축소 알고리즘.
 * "얼마나 줄이는지"와 "어떻게 뽑아내는지"를 여기 한 클래스에서 검증한다.
 */
class RouteCoordinateReductionServiceTest {

    private final RouteCoordinateReductionService service = new RouteCoordinateReductionService();

    private static Route route(long totalDistanceMeters, List<Double> vertexes) {
        return new Route(0, new Summary(totalDistanceMeters, 0L, null), List.of(new Section(List.of(new Road(vertexes)))));
    }

    private static List<Double> sequentialVertexes(int count) {
        List<Double> vertexes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            vertexes.add(127.0 + i * 0.0001);
            vertexes.add(37.0 + i * 0.0001);
        }
        return vertexes;
    }

    @Test
    @DisplayName("[경도,위도,...] 평탄 배열을 좌표쌍으로 펼친다")
    void reduce_flattensVertexes() {
        RouteGeometry geometry = service.reduce(route(0L, List.of(127.0, 37.0, 127.1, 37.1)));

        RoutePath path = geometry.path();
        assertThat(path.points()).hasSize(2);
        assertThat(path.points().get(0).longitude()).isEqualTo(127.0);
        assertThat(path.points().get(0).latitude()).isEqualTo(37.0);
        assertThat(path.points().get(1).longitude()).isEqualTo(127.1);
    }

    @Test
    @DisplayName("null sections/section/road/vertex를 안전하게 건너뛴다")
    void reduce_handlesNulls() {
        Route noSections = new Route(0, new Summary(0L, 0L, null), null);
        assertThat(service.reduce(noSections).path().isEmpty()).isTrue();

        List<Section> sectionsWithNulls = new ArrayList<>();
        sectionsWithNulls.add(null);
        sectionsWithNulls.add(new Section(null));
        sectionsWithNulls.add(new Section(sectionWithNullRoad()));
        Route route = new Route(0, new Summary(0L, 0L, null), sectionsWithNulls);
        assertThat(service.reduce(route).path().isEmpty()).isTrue();

        List<Double> withNull = Arrays.asList(null, 37.0, 127.0, null, 127.1, 37.1);
        assertThat(service.reduce(route(0L, withNull)).path().points()).hasSize(1);
    }

    private static List<Road> sectionWithNullRoad() {
        List<Road> roads = new ArrayList<>();
        roads.add(null);
        roads.add(new Road(null));
        return roads;
    }

    @Test
    @DisplayName("총 거리가 길면 200m 기준으로 300개보다 많이 남기고, 시작/끝을 보존한다")
    void reduce_reducesToDistanceBasedTargetForLongRoute() {
        List<Double> vertexes = sequentialVertexes(4000);
        RoutePath path = service.reduce(route(400_000L, vertexes)).path();

        assertThat(path.points()).hasSize(2000);
        assertThat(path.points().get(0).longitude()).isEqualTo(127.0);
        assertThat(path.points().get(1999).longitude()).isEqualTo(127.0 + 3999 * 0.0001);
    }

    @Test
    @DisplayName("총 거리가 짧아도 최소 300개는 유지한다")
    void reduce_keepsMinimumPointsForShortDistance() {
        List<Double> vertexes = sequentialVertexes(1000);
        RoutePath path = service.reduce(route(1_000L, vertexes)).path();

        assertThat(path.points()).hasSize(300);
    }

    @Test
    @DisplayName("거리 정보가 0이어도 최소 300개 기준으로 축소한다")
    void reduce_keepsMinimumPointsWhenDistanceIsZero() {
        List<Double> vertexes = sequentialVertexes(1000);
        RoutePath path = service.reduce(route(0L, vertexes)).path();

        assertThat(path.points()).hasSize(300);
    }

    @Test
    @DisplayName("원본 정점 수가 목표 개수보다 적으면 그대로 둔다")
    void reduce_keepsAllPointsWhenFewerThanTarget() {
        List<Double> vertexes = sequentialVertexes(50);
        RoutePath path = service.reduce(route(400_000L, vertexes)).path();

        assertThat(path.points()).hasSize(50);
    }

    @Test
    @DisplayName("각 좌표에 그 좌표가 속한 도로의 traffic_state를 채운다")
    void reduce_fillsTrafficStatePerRoad() {
        Road jamRoad = new Road("테헤란로", 24L, 9L, 9, 1, List.of(127.0, 37.0, 127.1, 37.1));
        Road smoothRoad = new Road("경부선", 500L, 20L, 90, 4, List.of(128.0, 38.0));
        Route route = new Route(0, new Summary(0L, 0L, null), List.of(new Section(List.of(jamRoad, smoothRoad))));

        RoutePath path = service.reduce(route).path();

        assertThat(path.trafficStateAt(0)).isEqualTo(1);
        assertThat(path.trafficStateAt(1)).isEqualTo(1);
        assertThat(path.trafficStateAt(2)).isEqualTo(4);
    }

    @Test
    @DisplayName("도로에 traffic_state가 없으면 trafficStateAt도 null이다")
    void reduce_leavesTrafficStateNullWhenRoadHasNone() {
        RoutePath path = service.reduce(route(0L, List.of(127.0, 37.0))).path();

        assertThat(path.trafficStateAt(0)).isNull();
    }
}
