package com.restroute.flight.controller.response;

public record FlightLegResponse(
        String airline,
        String flightNumber,
        String departureTime,
        String departureAirport,
        String arrivalTime,
        String arrivalAirport,
        int durationMinutes,
        String transferAirport,
        Integer layoverMinutes) {}
