package com.restroute.route.service;

import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.RestStopQueryService;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.route.service.RouteResolverService.RawRouteResult;
import com.restroute.route.service.dto.ResolvedRoute.RouteGeometry;
import com.restroute.route.service.dto.RouteCandidate;
import com.restroute.route.service.dto.RouteCandidates;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 출발지와 목적지를 주면 그 경로 위 후보 휴게소를 찾는다. 길찾기 호출부터 반경·방향 매칭까지가
 * 이 인터페이스 뒤에 있고, 호출부는 좌표와 목적지만 안다.
 *
 * <p>후보는 집계가 조합되기 전 단계다 — 어떤 정보를 얹어 응답으로 만들지는 호출부가 정한다.
 */
@Service
@RequiredArgsConstructor
public class RouteCandidateFinder {

    private final RouteResolverService routeResolverService;
    private final RestStopQueryService restStopQueryService;
    private final RouteCoordinateReducer routeCoordinateReducer;
    private final RouteRestStopMatcher routeRestStopMatcher;

    public RouteCandidates find(double originLatitude, double originLongitude, Destination destination) {
        RawRouteResult raw = routeResolverService.resolveRoute(originLatitude, originLongitude, destination);
        List<RouteGeometry> routes = reduceToNonEmptyPaths(raw.routes());

        List<RestStopEntity> allRestStops = restStopQueryService.findAll();
        List<RouteCandidate> candidates = IntStream.range(0, routes.size())
                .mapToObj(routeIndex -> toCandidate(routeIndex, routes.get(routeIndex), allRestStops))
                .toList();

        return new RouteCandidates(candidates, allRestStops);
    }

    /**
     * 좌표를 줄이는 것이 휴게소 조회보다 먼저다 — 경로가 비어 끝날 요청에서 휴게소 전체를
     * 읽지 않는다.
     */
    private List<RouteGeometry> reduceToNonEmptyPaths(List<KakaoDirectionsResponse.Route> rawRoutes) {
        List<RouteGeometry> routes = rawRoutes.stream()
                .map(routeCoordinateReducer::reduce)
                .filter(geometry -> !geometry.path().isEmpty())
                .toList();
        if (routes.isEmpty()) {
            throw RouteRestStopNotFoundException.emptyRoutePath();
        }
        return routes;
    }

    private RouteCandidate toCandidate(int routeIndex, RouteGeometry geometry, List<RestStopEntity> allRestStops) {
        List<RouteRestStopItem> items = routeRestStopMatcher.match(geometry.path(), allRestStops);
        return new RouteCandidate(routeIndex, geometry, items);
    }
}
