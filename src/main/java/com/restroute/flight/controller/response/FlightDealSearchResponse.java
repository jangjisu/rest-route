package com.restroute.flight.controller.response;

import java.util.List;

public record FlightDealSearchResponse(List<FlightDealResponse> items, FlightDealSearchMeta meta) {}
