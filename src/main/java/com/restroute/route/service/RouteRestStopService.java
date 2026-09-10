package com.restroute.route.service;

import com.restroute.oilprice.dto.NationalOilPriceSummary;
import com.restroute.oilprice.service.NationalOilPriceService;
import com.restroute.route.controller.response.RouteRestStopResponse;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteOption;
import com.restroute.route.service.dto.RouteCandidates;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 지도 화면의 경로 휴게소 검색. 목적지를 정하고, 경로 위 후보를 찾고, 후보에 상세 정보를 얹는다.
 */
@Service
@RequiredArgsConstructor
public class RouteRestStopService {

    private final RouteResolverService routeResolverService;
    private final RouteCandidateFinder routeCandidateFinder;
    private final NationalOilPriceService nationalOilPriceService;
    private final RouteOptionAssemblyService routeOptionAssemblyService;

    public RouteRestStopResponse findRouteRestStops(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName) {
        Destination destination = routeResolverService.resolveDestination(
                destinationQuery, destinationLatitude, destinationLongitude, destinationName);
        RouteCandidates found = routeCandidateFinder.find(originLatitude, originLongitude, destination);

        Optional<NationalOilPriceSummary> nationalOilPriceSummary = nationalOilPriceService.getTodaySummary();
        List<RouteOption> routeOptions = routeOptionAssemblyService.attachDetails(
                found.candidates(), found.allRestStops(), nationalOilPriceSummary);

        return RouteRestStopResponse.of(destination, routeOptions);
    }
}
