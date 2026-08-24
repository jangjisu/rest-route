package com.restroute.service.route;

import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.controller.response.RouteRestStopResponse;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.controller.response.RouteRestStopResponse.RouteOption;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.NationalOilPriceService;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.route.RouteResolverService.RawRouteResult;
import com.restroute.service.route.dto.ResolvedRoute.RouteGeometry;
import com.restroute.service.route.dto.RouteCandidate;
import com.restroute.service.route.exception.RouteRestStopNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 경로 휴게소 검색의 전체 흐름을 순서대로 보여주는 오케스트레이터.
 * 1) 좌표 얻기 → 2) 좌표 개수 줄이기 → 3) 방향 매칭 → 4) detail 조립 순으로 진행한다.
 */
@Service
@RequiredArgsConstructor
public class RouteRestStopService {

    private final RouteResolverService routeResolverService;
    private final RestStopQueryService restStopQueryService;
    private final NationalOilPriceService nationalOilPriceService;
    private final RouteCoordinateReducer routeCoordinateReducer;
    private final RouteRestStopMatcher routeRestStopMatcher;
    private final RouteOptionAssemblyService routeOptionAssemblyService;

    public RouteRestStopResponse findRouteRestStops(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName,
            int radiusMeters) {
        RawRouteResult raw = routeResolverService.resolveDestinationAndRoute(
                originLatitude,
                originLongitude,
                destinationQuery,
                destinationLatitude,
                destinationLongitude,
                destinationName); // 1. 출발지/도착지 좌표 얻기

        List<RouteGeometry> routes = reduceCoordinates(raw.routes()); // 2. 좌표 개수 줄이기

        List<RestStopEntity> allRestStops = restStopQueryService.findAll();
        List<RouteCandidate> candidates = matchRestStopsByDirection(routes, allRestStops, radiusMeters); // 3. 방향 매칭

        Optional<NationalOilPriceSummary> nationalOilPriceSummary = nationalOilPriceService.getTodaySummary();
        List<RouteOption> routeOptions = routeOptionAssemblyService.attachDetails(
                candidates, allRestStops, nationalOilPriceSummary); // 4. detail 조립

        return RouteRestStopResponse.of(raw.destination(), routeOptions);
    }

    /**
     * 카카오가 준 원본 폴리라인은 총 거리에 비해 정점이 너무 많다 — 근접거리 계산(nearestTo)이
     * 매 휴게소마다 전체 정점을 순회하므로, 성능을 위해 여기서 미리 정점 수를 줄인다
     * (RouteCoordinateReducer에 위임 — 200m 간격/최소 300개 기준 균등 샘플링).
     * 개별 경로의 좌표가 비어있으면 그 경로만 제외하고, 전부 비어있으면 예외로 끝낸다.
     */
    private List<RouteGeometry> reduceCoordinates(List<KakaoDirectionsResponse.Route> rawRoutes) {
        List<RouteGeometry> routes = rawRoutes.stream()
                .map(routeCoordinateReducer::reduce)
                .filter(geometry -> !geometry.path().isEmpty())
                .toList();
        if (routes.isEmpty()) {
            throw new RouteRestStopNotFoundException("경로 좌표가 없습니다.");
        }
        return routes;
    }

    /**
     * 대안 경로마다 독립적으로, 휴게소 전체를 경로 반경 안에서 매칭하고 상·하행 페어의
     * 진행방향을 판별한다 (RouteRestStopMatcher에 위임).
     */
    private List<RouteCandidate> matchRestStopsByDirection(
            List<RouteGeometry> routes, List<RestStopEntity> allRestStops, int radiusMeters) {
        return IntStream.range(0, routes.size())
                .mapToObj(routeIndex -> {
                    RouteGeometry geometry = routes.get(routeIndex);
                    List<RouteRestStopItem> items =
                            routeRestStopMatcher.match(geometry.path(), radiusMeters, allRestStops);
                    return new RouteCandidate(routeIndex, geometry, items);
                })
                .toList();
    }
}
