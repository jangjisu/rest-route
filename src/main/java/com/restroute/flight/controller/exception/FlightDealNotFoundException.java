package com.restroute.flight.controller.exception;

public class FlightDealNotFoundException extends RuntimeException {

    public FlightDealNotFoundException(String cursor) {
        super("Deal not found or already expired: cursor=" + cursor);
    }
}
