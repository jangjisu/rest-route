package com.restroute.service.route.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 좌표 축소(다운샘플링) 자체는 RouteCoordinateReducerTest에서 검증한다.
 * 여기서는 이미 축소된 좌표열에 대한 근접·방향 계산만 검증한다.
 */
class RoutePathTest {

    private static RoutePath path(List<Double> lngLatPairs) {
        List<PathPoint> points = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < lngLatPairs.size(); i += 2) {
            points.add(new PathPoint(lngLatPairs.get(i), lngLatPairs.get(i + 1)));
        }
        return RoutePath.of(points, Arrays.asList(new Integer[points.size()]));
    }

    @Test
    void nearestTo_returnsClosest() {
        RoutePath path = path(List.of(127.0, 37.0, 128.0, 38.0));

        RoutePath.Nearest near = path.nearestTo(37.0001, 127.0001);
        assertThat(near.index()).isEqualTo(0);
        assertThat(near.distanceMeters()).isLessThan(50);

        RoutePath.Nearest far = path.nearestTo(38.0, 128.0);
        assertThat(far.index()).isEqualTo(1);
        assertThat(far.distanceMeters()).isLessThan(1);
    }

    private static RoutePath northHeadingPath() {
        List<Double> vertexes = new java.util.ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            vertexes.add(127.0);
            vertexes.add(37.000 + i * 0.001);
        }
        return path(vertexes);
    }

    @Test
    void sideOfTravel_returnsRightForEastPoint() {
        RoutePath path = northHeadingPath();
        RoutePath.Nearest nearest = path.nearestTo(37.005, 127.001);

        assertThat(path.sideOfTravel(nearest.index(), 37.005, 127.001)).isEqualTo(RoutePath.Side.RIGHT);
    }

    @Test
    void sideOfTravel_returnsLeftForWestPoint() {
        RoutePath path = northHeadingPath();
        RoutePath.Nearest nearest = path.nearestTo(37.005, 126.999);

        assertThat(path.sideOfTravel(nearest.index(), 37.005, 126.999)).isEqualTo(RoutePath.Side.LEFT);
    }

    @Test
    void sideOfTravel_worksNearPathBoundaries() {
        RoutePath path = northHeadingPath();

        assertThat(path.sideOfTravel(0, 37.000, 127.001)).isEqualTo(RoutePath.Side.RIGHT);
        assertThat(path.sideOfTravel(10, 37.010, 126.999)).isEqualTo(RoutePath.Side.LEFT);
    }

    @Test
    void sideOfTravel_handlesDiagonalHeading() {
        List<Double> vertexes = new java.util.ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            vertexes.add(127.000 + i * 0.001);
            vertexes.add(37.000 + i * 0.001);
        }
        RoutePath path = path(vertexes);

        assertThat(path.sideOfTravel(5, 37.004, 127.006)).isEqualTo(RoutePath.Side.RIGHT);
        assertThat(path.sideOfTravel(5, 37.006, 127.004)).isEqualTo(RoutePath.Side.LEFT);
    }

    @Test
    void sideOfTravel_returnsUnknownWhenDirectionIsDegenerate() {
        RoutePath singlePoint = path(List.of(127.0, 37.0));

        assertThat(singlePoint.sideOfTravel(0, 37.001, 127.001)).isEqualTo(RoutePath.Side.UNKNOWN);
    }

    @Test
    void path_returnsLongitudeLatitudePairs() {
        RoutePath path = path(List.of(127.0, 37.0, 127.1, 37.1));

        assertThat(path.path()).containsExactly(List.of(127.0, 37.0), List.of(127.1, 37.1));
    }

    @Test
    void trafficStateAt_returnsValueGivenAtConstruction() {
        RoutePath path = RoutePath.of(
                List.of(new PathPoint(127.0, 37.0), new PathPoint(127.1, 37.1)), Arrays.asList(1, null));

        assertThat(path.trafficStateAt(0)).isEqualTo(1);
        assertThat(path.trafficStateAt(1)).isNull();
    }

    @Test
    void isEmpty_trueWhenNoPoints() {
        assertThat(RoutePath.of(List.of(), List.of()).isEmpty()).isTrue();
    }
}
