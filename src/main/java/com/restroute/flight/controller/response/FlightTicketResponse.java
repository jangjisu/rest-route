package com.restroute.flight.controller.response;

import java.util.List;

public record FlightTicketResponse(
        boolean bestPrice,
        String destinationCity,
        String destinationAirport,
        String departureDate,
        String returnDate,
        int nights,
        int transferCount,
        FlightLegResponse outbound,
        FlightLegResponse inbound,
        List<String> baggageInfo,
        int price,
        String currency,
        String passengerLabel,
        String gateName,
        String bookingLink) {}
