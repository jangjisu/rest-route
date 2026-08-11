package com.restroute.flight.controller.response;

public record FlightDealResponse(String id, Destination destination, String departureAt, Price price) {

    public record Destination(String iata, String city) {}

    public record Price(int amount, String currency) {}
}
