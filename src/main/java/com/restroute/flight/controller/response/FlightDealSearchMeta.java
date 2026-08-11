package com.restroute.flight.controller.response;

public record FlightDealSearchMeta(String nextCursor, boolean hasNext, int totalCount) {}
