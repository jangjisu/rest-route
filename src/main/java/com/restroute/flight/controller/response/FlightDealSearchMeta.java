package com.restroute.flight.controller.response;

public record FlightDealSearchMeta(
        int totalCount, String nextCursor, boolean hasNext, String locale, String currency, String fetchedAt) {

    public static FlightDealSearchMeta of(
            String nextCursor, boolean hasNext, int totalCount, String locale, String currency, String fetchedAt) {
        return new FlightDealSearchMeta(totalCount, nextCursor, hasNext, locale, currency, fetchedAt);
    }
}
