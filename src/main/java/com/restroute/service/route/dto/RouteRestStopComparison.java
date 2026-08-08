package com.restroute.service.route.dto;

import com.restroute.controller.response.RouteRestStopResponse.ComparisonSummary;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;

public record RouteRestStopComparison(RouteRestStopItem item, ComparisonSummary summary) {

    public static RouteRestStopComparison of(RouteRestStopItem item, ComparisonSummary summary) {
        return new RouteRestStopComparison(item, summary);
    }
}
