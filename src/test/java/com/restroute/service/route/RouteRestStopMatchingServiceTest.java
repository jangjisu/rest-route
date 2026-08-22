package com.restroute.service.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.route.dto.PathPoint;
import com.restroute.service.route.dto.RoutePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteRestStopMatchingServiceTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5, 128.0, 38.0);
    private static final List<Double> NORTH_HEADING_VERTEXES = List.of(127.0, 37.0, 127.0, 37.01);

    private final RouteRestStopMatchingService service = new RouteRestStopMatchingService();

    private RoutePath path(List<Double> lngLatPairs) {
        List<PathPoint> points = new ArrayList<>();
        for (int i = 0; i + 1 < lngLatPairs.size(); i += 2) {
            points.add(new PathPoint(lngLatPairs.get(i), lngLatPairs.get(i + 1)));
        }
        return RoutePath.of(points, Arrays.asList(new Integer[points.size()]));
    }

    private RestStopEntity restStop(String code, String name, String route, String lng, String lat) {
        RestStopEntity entity = mock(RestStopEntity.class);
        lenient().when(entity.getServiceAreaCode()).thenReturn(code);
        lenient().when(entity.getUnitName()).thenReturn(name);
        lenient().when(entity.getRouteName()).thenReturn(route);
        lenient().when(entity.getXValue()).thenReturn(lng);
        lenient().when(entity.getYValue()).thenReturn(lat);
        return entity;
    }

    @Test
    void keepsRestStopsWithinRadius_inRouteOrder() {
        RestStopEntity near1 = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        RestStopEntity near0 = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity near2 = restStop("C", "C휴게소", "경부선", "128.0001", "38.0001");
        RestStopEntity far = restStop("C", "C휴게소", "중부선", "130.0", "40.0");

        List<RouteRestStopItem> items =
                service.match(path(VERTEXES), 1000, List.of(near1, near0, near2, far));

        assertThat(items)
                .extracting(RouteRestStopItem::serviceAreaCode)
                .containsExactly("A", "B", "C");
        assertThat(items.get(0).distanceFromRouteMeters()).isLessThan(50L);
    }

    @Test
    void excludesInvalidOrOutOfRadiusCoordinates() {
        RestStopEntity nullLatitude = restStop("A", "A", "x", "127.0", null);
        RestStopEntity nullLongitude = restStop("B", "B", "x", null, "37.0");
        RestStopEntity blank = restStop("D", "D", "x", "127.0", "   ");
        RestStopEntity nonNumeric = restStop("E", "E", "x", "127.0", "abc");
        RestStopEntity far = restStop("C", "C휴게소", "중부선", "130.0", "40.0");

        List<RouteRestStopItem> items = service.match(
                path(VERTEXES), 1000, List.of(nullLatitude, nullLongitude, blank, nonNumeric, far));

        assertThat(items).isEmpty();
    }

    @Test
    void directionPair_keepsOnlyReachableSide() {
        RestStopEntity busan = restStop("A", "안성(부산)휴게소", "경부선", "127.001", "37.005");
        RestStopEntity seoul = restStop("B", "안성(서울)휴게소", "경부선", "126.999", "37.005");

        List<RouteRestStopItem> items =
                service.match(path(NORTH_HEADING_VERTEXES), 1000, List.of(busan, seoul));

        assertThat(items).extracting(RouteRestStopItem::serviceAreaCode).containsExactly("A");
        assertThat(items.get(0).hasDirectionAlternative()).isFalse();
    }

    @Test
    void soloRestStop_survivesRegardlessOfSide() {
        RestStopEntity majang = restStop("C", "마장휴게소", "중부선", "126.999", "37.005");

        List<RouteRestStopItem> items = service.match(path(NORTH_HEADING_VERTEXES), 1000, List.of(majang));

        assertThat(items).extracting(RouteRestStopItem::serviceAreaCode).containsExactly("C");
        assertThat(items.get(0).hasDirectionAlternative()).isFalse();
    }

    @Test
    void directionPairNameWithoutNearbySibling_survivesUnfiltered() {
        RestStopEntity busan = restStop("A", "안성(부산)휴게소", "경부선", "127.001", "37.005");

        List<RouteRestStopItem> items = service.match(path(NORTH_HEADING_VERTEXES), 1000, List.of(busan));

        assertThat(items).extracting(RouteRestStopItem::serviceAreaCode).containsExactly("A");
        assertThat(items.get(0).hasDirectionAlternative()).isFalse();
    }

    @Test
    void ambiguousDirectionPair_keepsBothAndMarksAlternative() {
        RestStopEntity busan = restStop("A", "죽암(부산)휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity seoul = restStop("B", "죽암(서울)휴게소", "경부선", "126.9999", "37.0001");

        List<RouteRestStopItem> items =
                service.match(path(List.of(127.0, 37.0)), 1000, List.of(busan, seoul));

        assertThat(items).extracting(RouteRestStopItem::serviceAreaCode).containsExactlyInAnyOrder("A", "B");
        assertThat(items).allSatisfy(item -> assertThat(item.hasDirectionAlternative()).isTrue());
    }

    @Test
    void malformedDirectionLabels_areHandledAsIndependentCandidates() {
        RestStopEntity blankDirection = restStop("A", "화성()휴게소", "서해안선", "127.5001", "37.5001");
        RestStopEntity unnamedNear = restStop("B", null, "서해안선", "127.0001", "37.0001");
        RestStopEntity unnamedFar = restStop("B", null, "서해안선", "127.005", "37.005");

        List<RouteRestStopItem> items =
                service.match(path(VERTEXES), 1000, List.of(blankDirection, unnamedFar, unnamedNear));

        assertThat(items)
                .extracting(RouteRestStopItem::unitName)
                .containsExactly(null, null, "화성()휴게소");
        assertThat(items).extracting(RouteRestStopItem::hasDirectionAlternative).containsExactly(false, false, false);
    }

    @Test
    void nearbyTraffic_readsFromNearestRoadSegment() {
        RoutePath path = RoutePath.of(
                List.of(new PathPoint(127.0, 37.0), new PathPoint(127.5, 37.5), new PathPoint(128.0, 38.0)),
                Arrays.asList(1, 4, null));

        RestStopEntity near0 = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity near1 = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        RestStopEntity near2 = restStop("C", "C휴게소", "경부선", "128.0001", "38.0001");

        List<RouteRestStopItem> items = service.match(path, 1000, List.of(near0, near1, near2));

        assertThat(items.get(0).nearbyTraffic().key()).isEqualTo("jam");
        assertThat(items.get(0).nearbyTraffic().label()).isEqualTo("정체");
        assertThat(items.get(1).nearbyTraffic().key()).isEqualTo("smooth");
        assertThat(items.get(2).nearbyTraffic()).isNull();
    }
}
