package com.restroute.flight.controller.response;

import java.util.List;

public record FlightSearchResultResponse(int totalCount, String priceAsOf, List<FlightTicketResponse> tickets) {}
