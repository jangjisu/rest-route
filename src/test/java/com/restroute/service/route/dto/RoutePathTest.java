package com.restroute.service.route.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.client.response.KakaoDirectionsResponse.Road;
import com.restroute.client.response.KakaoDirectionsResponse.Route;
import com.restroute.client.response.KakaoDirectionsResponse.Section;
import com.restroute.client.response.KakaoDirectionsResponse.Summary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoutePathTest {

    private static Route route(List<Double> vertexes) {
        return new Route(0, null, List.of(new Section(List.of(new Road(vertexes)))));
    }

    private static Route routeWithDistance(List<Double> vertexes, long distanceMeters) {
        return new Route(0, new Summary(distanceMeters, 0L), List.of(new Section(List.of(new Road(vertexes)))));
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
    @DisplayName("from은 [경도,위도,...] 평탄 배열을 좌표쌍으로 펼친다")
    void from_flattensVertexes() {
        RoutePath path = RoutePath.from(route(List.of(127.0, 37.0, 127.1, 37.1)));

        assertThat(path.points()).hasSize(2);
        assertThat(path.points().get(0).longitude()).isEqualTo(127.0);
        assertThat(path.points().get(0).latitude()).isEqualTo(37.0);
        assertThat(path.points().get(1).longitude()).isEqualTo(127.1);
    }

    @Test
    @DisplayName("from은 null route/section/road/vertex를 안전하게 건너뛴다")
    void from_handlesNulls() {
        assertThat(RoutePath.from(null).isEmpty()).isTrue();
        assertThat(RoutePath.from(new Route(0, null, null)).isEmpty()).isTrue();

        List<Section> sections = new ArrayList<>();
        sections.add(null);
        sections.add(new Section(null));
        sections.add(new Section(sectionWithNullRoad()));
        assertThat(RoutePath.from(new Route(0, null, sections)).isEmpty()).isTrue();

        List<Double> withNull = Arrays.asList(null, 37.0, 127.0, null, 127.1, 37.1);
        assertThat(RoutePath.from(route(withNull)).points()).hasSize(1);
    }

    private static List<Road> sectionWithNullRoad() {
        List<Road> roads = new ArrayList<>();
        roads.add(null);
        roads.add(new Road(null));
        return roads;
    }

    @Test
    @DisplayName("총 거리가 길면 200m 기준으로 300개보다 많이 남기고, 시작/끝을 보존한다")
    void from_reducesToDistanceBasedTargetForLongRoute() {
        List<Double> vertexes = sequentialVertexes(4000);
        RoutePath path = RoutePath.from(routeWithDistance(vertexes, 400_000L));

        assertThat(path.points()).hasSize(2000);
        assertThat(path.points().get(0).longitude()).isEqualTo(127.0);
        assertThat(path.points().get(1999).longitude()).isEqualTo(127.0 + 3999 * 0.0001);
    }

    @Test
    @DisplayName("총 거리가 짧아도 최소 300개는 유지한다")
    void from_keepsMinimumPointsForShortDistance() {
        List<Double> vertexes = sequentialVertexes(1000);
        RoutePath path = RoutePath.from(routeWithDistance(vertexes, 1_000L));

        assertThat(path.points()).hasSize(300);
    }

    @Test
    @DisplayName("summary가 없어 거리 정보가 없어도 최소 300개 기준으로 축소한다")
    void from_keepsMinimumPointsWhenSummaryMissing() {
        List<Double> vertexes = sequentialVertexes(1000);
        RoutePath path = RoutePath.from(route(vertexes));

        assertThat(path.points()).hasSize(300);
    }

    @Test
    @DisplayName("원본 정점 수가 목표 개수보다 적으면 그대로 둔다")
    void from_keepsAllPointsWhenFewerThanTarget() {
        List<Double> vertexes = sequentialVertexes(50);
        RoutePath path = RoutePath.from(routeWithDistance(vertexes, 400_000L));

        assertThat(path.points()).hasSize(50);
    }

    @Test
    @DisplayName("from은 각 좌표에 그 좌표가 속한 도로의 traffic_state를 채운다")
    void from_fillsTrafficStatePerRoad() {
        Road jamRoad = new Road("테헤란로", 24L, 9L, 9, 1, List.of(127.0, 37.0, 127.1, 37.1));
        Road smoothRoad = new Road("경부선", 500L, 20L, 90, 4, List.of(128.0, 38.0));
        Route route = new Route(0, null, List.of(new Section(List.of(jamRoad, smoothRoad))));

        RoutePath path = RoutePath.from(route);

        assertThat(path.trafficStateAt(0)).isEqualTo(1);
        assertThat(path.trafficStateAt(1)).isEqualTo(1);
        assertThat(path.trafficStateAt(2)).isEqualTo(4);
    }

    @Test
    @DisplayName("도로에 traffic_state가 없으면 trafficStateAt도 null이다")
    void from_leavesTrafficStateNullWhenRoadHasNone() {
        RoutePath path = RoutePath.from(route(List.of(127.0, 37.0)));

        assertThat(path.trafficStateAt(0)).isNull();
    }

    @Test
    @DisplayName("nearestTo는 최단거리(m)와 가장 가까운 정점 인덱스를 반환한다")
    void nearestTo_returnsClosest() {
        RoutePath path = RoutePath.from(route(List.of(127.0, 37.0, 128.0, 38.0)));

        RoutePath.Nearest near = path.nearestTo(37.0001, 127.0001);
        assertThat(near.index()).isEqualTo(0);
        assertThat(near.distanceMeters()).isLessThan(50);

        RoutePath.Nearest far = path.nearestTo(38.0, 128.0);
        assertThat(far.index()).isEqualTo(1);
        assertThat(far.distanceMeters()).isLessThan(1);
    }

    private static RoutePath northHeadingPath() {
        List<Double> vertexes = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            vertexes.add(127.0);
            vertexes.add(37.000 + i * 0.001);
        }
        return RoutePath.from(route(vertexes));
    }

    @Test
    @DisplayName("sideOfTravel은 북쪽으로 향하는 경로에서 동쪽(진행방향 오른쪽) 지점을 RIGHT로 판별한다")
    void sideOfTravel_returnsRightForEastPoint() {
        RoutePath path = northHeadingPath();
        RoutePath.Nearest nearest = path.nearestTo(37.005, 127.001);

        assertThat(path.sideOfTravel(nearest.index(), 37.005, 127.001)).isEqualTo(RoutePath.Side.RIGHT);
    }

    @Test
    @DisplayName("sideOfTravel은 북쪽으로 향하는 경로에서 서쪽(진행방향 왼쪽) 지점을 LEFT로 판별한다")
    void sideOfTravel_returnsLeftForWestPoint() {
        RoutePath path = northHeadingPath();
        RoutePath.Nearest nearest = path.nearestTo(37.005, 126.999);

        assertThat(path.sideOfTravel(nearest.index(), 37.005, 126.999)).isEqualTo(RoutePath.Side.LEFT);
    }

    @Test
    @DisplayName("sideOfTravel은 경로 시작/끝 근처(인덱스 경계)에서도 좌/우를 판별한다")
    void sideOfTravel_worksNearPathBoundaries() {
        RoutePath path = northHeadingPath();

        assertThat(path.sideOfTravel(0, 37.000, 127.001)).isEqualTo(RoutePath.Side.RIGHT);
        assertThat(path.sideOfTravel(10, 37.010, 126.999)).isEqualTo(RoutePath.Side.LEFT);
    }

    @Test
    @DisplayName("sideOfTravel은 경도도 함께 변하는 대각선 경로에서도 좌/우를 판별한다")
    void sideOfTravel_handlesDiagonalHeading() {
        List<Double> vertexes = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            vertexes.add(127.000 + i * 0.001);
            vertexes.add(37.000 + i * 0.001);
        }
        RoutePath path = RoutePath.from(route(vertexes));

        assertThat(path.sideOfTravel(5, 37.004, 127.006)).isEqualTo(RoutePath.Side.RIGHT);
        assertThat(path.sideOfTravel(5, 37.006, 127.004)).isEqualTo(RoutePath.Side.LEFT);
    }

    @Test
    @DisplayName("sideOfTravel은 진행방향 벡터가 축퇴(같은 지점 반복)되면 UNKNOWN을 반환한다")
    void sideOfTravel_returnsUnknownWhenDirectionIsDegenerate() {
        RoutePath singlePoint = RoutePath.from(route(List.of(127.0, 37.0)));

        assertThat(singlePoint.sideOfTravel(0, 37.001, 127.001)).isEqualTo(RoutePath.Side.UNKNOWN);
    }

    @Test
    @DisplayName("path()는 [경도,위도] 쌍 목록을 반환한다")
    void path_returnsLongitudeLatitudePairs() {
        RoutePath path = RoutePath.from(route(List.of(127.0, 37.0, 127.1, 37.1)));

        assertThat(path.path()).containsExactly(List.of(127.0, 37.0), List.of(127.1, 37.1));
    }
}
