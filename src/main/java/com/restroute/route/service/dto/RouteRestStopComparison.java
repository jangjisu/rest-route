package com.restroute.route.service.dto;

import com.restroute.route.controller.response.RouteRestStopResponse.ComparisonSummary;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;

public record RouteRestStopComparison(RouteRestStopItem item, ComparisonSummary summary) {

    public static RouteRestStopComparison of(RouteRestStopItem item, ComparisonSummary summary) {
        return new RouteRestStopComparison(item, summary);
    }
}
