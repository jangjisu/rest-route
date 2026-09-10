package com.restroute.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.restroute.common.client.response.KakaoDirectionsResponse.Road;
import com.restroute.common.client.response.KakaoDirectionsResponse.Route;
import com.restroute.common.client.response.KakaoDirectionsResponse.Section;
import com.restroute.common.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.RestStopQueryService;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.route.service.RouteResolverService.RawRouteResult;
import com.restroute.route.service.dto.RouteCandidate;
import com.restroute.route.service.dto.RouteCandidates;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteCandidateFinderTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5);
    private static final Destination DESTINATION = Destination.of("부산역", 35.0, 129.0);

    @Mock
    private RouteResolverService routeResolverService;

    @Mock
    private RestStopQueryService restStopQueryService;

    private RouteCandidateFinder finder;

    @BeforeEach
    void setUp() {
        finder = new RouteCandidateFinder(
                routeResolverService,
                restStopQueryService,
                new RouteCoordinateReducer(),
                new RouteRestStopMatcher(1000));
    }

    @Test
    @DisplayName("경로마다 인덱스를 매겨 후보를 만든다")
    void find_indexesEachRoute() {
        stubRoutes(route(VERTEXES), route(VERTEXES));
        when(restStopQueryService.findAll()).thenReturn(List.of());

        RouteCandidates result = finder.find(37.0, 127.0, DESTINATION);

        assertThat(result.candidates()).extracting(RouteCandidate::routeIndex).containsExactly(0, 1);
    }

    @Test
    @DisplayName("반경 안 휴게소를 그 경로의 후보로 담는다")
    void find_attachesMatchedRestStops() {
        stubRoutes(route(VERTEXES));
        RestStopEntity nearby = restStop("A", "127.0001", "37.0001");
        when(restStopQueryService.findAll()).thenReturn(List.of(nearby));

        RouteCandidates result = finder.find(37.0, 127.0, DESTINATION);

        assertThat(result.first().items())
                .extracting(RouteRestStopItem::serviceAreaCode)
                .containsExactly("A");
    }

    @Test
    @DisplayName("후보를 찾는 데 쓴 휴게소 전체를 함께 돌려준다")
    void find_returnsAllRestStopsItSearched() {
        stubRoutes(route(VERTEXES));
        RestStopEntity faraway = restStop("B", "128.9", "38.9");
        when(restStopQueryService.findAll()).thenReturn(List.of(faraway));

        RouteCandidates result = finder.find(37.0, 127.0, DESTINATION);

        assertThat(result.allRestStops()).containsExactly(faraway);
        assertThat(result.first().items()).isEmpty();
    }

    @Test
    @DisplayName("좌표가 하나도 없는 경로만 오면 NotFound이고 휴게소를 조회하지 않는다")
    void find_throwsWhenEveryRoutePathIsEmpty() {
        stubRoutes(route(List.of()));

        assertThatThrownBy(() -> finder.find(37.0, 127.0, DESTINATION))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        verifyNoInteractions(restStopQueryService);
    }

    private void stubRoutes(Route... routes) {
        lenient()
                .when(routeResolverService.resolveRoute(anyDouble(), anyDouble(), any()))
                .thenReturn(new RawRouteResult(DESTINATION, List.of(routes)));
    }

    private Route route(List<Double> vertexes) {
        return new Route(0, new Summary(100L, 200L, null), List.of(new Section(List.of(new Road(vertexes)))));
    }

    private RestStopEntity restStop(String code, String longitude, String latitude) {
        RestStopEntity entity = mock(RestStopEntity.class);
        lenient().when(entity.getServiceAreaCode()).thenReturn(code);
        lenient().when(entity.getUnitName()).thenReturn(code + "휴게소");
        lenient().when(entity.getRouteName()).thenReturn("경부선");
        lenient().when(entity.getRouteNo()).thenReturn("0010");
        lenient().when(entity.getXValue()).thenReturn(longitude);
        lenient().when(entity.getYValue()).thenReturn(latitude);
        return entity;
    }
}
