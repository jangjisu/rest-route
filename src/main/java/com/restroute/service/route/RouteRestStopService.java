package com.restroute.service.route;

import com.restroute.controller.response.RouteRestStopResponse;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.controller.response.RouteRestStopResponse.RouteOption;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.NationalOilPriceService;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.route.dto.ResolvedRoute;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteRestStopService {

    private final RouteResolverService routeResolverService;
    private final RestStopQueryService restStopQueryService;
    private final NationalOilPriceService nationalOilPriceService;
    private final RouteOptionAssemblyService routeOptionAssemblyService;

    public RouteRestStopResponse findRouteRestStops(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName,
            int radiusMeters) {
        ResolvedRoute resolved = routeResolverService.resolve(
                originLatitude,
                originLongitude,
                destinationQuery,
                destinationLatitude,
                destinationLongitude,
                destinationName);

        List<RestStopEntity> allRestStops = restStopQueryService.findAll();
        Optional<NationalOilPriceSummary> nationalOilPriceSummary = nationalOilPriceService.getTodaySummary();

        List<RouteOption> routes = routeOptionAssemblyService.assemble(
                resolved.routes(), allRestStops, radiusMeters, nationalOilPriceSummary);

        return RouteRestStopResponse.of(resolved.destination(), routes);
    }
}
