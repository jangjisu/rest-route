package com.restroute.flight.controller.response;

public record FlightDealSearchMeta(String nextCursor, boolean hasNext, int totalCount) {

    public static FlightDealSearchMeta of(String nextCursor, boolean hasNext, int totalCount) {
        return new FlightDealSearchMeta(nextCursor, hasNext, totalCount);
    }
}
