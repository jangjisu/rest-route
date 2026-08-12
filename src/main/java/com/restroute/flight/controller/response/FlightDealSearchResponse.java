package com.restroute.flight.controller.response;

import java.util.List;

public record FlightDealSearchResponse(List<FlightDealResponse> items, FlightDealSearchMeta meta) {

    public static FlightDealSearchResponse of(List<FlightDealResponse> items, FlightDealSearchMeta meta) {
        return new FlightDealSearchResponse(items, meta);
    }
}
