package com.restroute.route.service.dto;

import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteSummary;
import java.util.List;

public record RouteSearchResult(
        Destination destination, RouteSummary routeSummary, List<RouteRestStopCandidate> candidates) {

    public static RouteSearchResult of(
            Destination destination, RouteSummary routeSummary, List<RouteRestStopCandidate> candidates) {
        return new RouteSearchResult(destination, routeSummary, candidates);
    }
}
