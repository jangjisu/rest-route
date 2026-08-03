package com.restroute.service.route.dto;

import com.restroute.controller.response.RouteRestStopResponse.ComparisonSummary;

public record RouteRestStopComparison(RouteRestStopCandidate candidate, ComparisonSummary summary) {

    public static RouteRestStopComparison of(RouteRestStopCandidate candidate, ComparisonSummary summary) {
        return new RouteRestStopComparison(candidate, summary);
    }
}
