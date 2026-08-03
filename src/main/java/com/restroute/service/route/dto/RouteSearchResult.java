package com.restroute.service.route.dto;

import com.restroute.controller.response.RouteRestStopResponse.Destination;
import com.restroute.controller.response.RouteRestStopResponse.RouteSummary;
import java.util.List;

public record RouteSearchResult(
        Destination destination, RouteSummary routeSummary, List<RouteRestStopCandidate> candidates) {

    public static RouteSearchResult of(
            Destination destination, RouteSummary routeSummary, List<RouteRestStopCandidate> candidates) {
        return new RouteSearchResult(destination, routeSummary, candidates);
    }
}
