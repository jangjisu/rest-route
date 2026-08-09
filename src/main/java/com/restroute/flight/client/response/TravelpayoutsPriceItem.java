package com.restroute.flight.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelpayoutsPriceItem(
        String origin,
        String destination,
        @JsonProperty("origin_airport") String originAirport,
        @JsonProperty("destination_airport") String destinationAirport,
        int price,
        String airline,
        @JsonProperty("flight_number") String flightNumber,
        @JsonProperty("departure_at") String departureAt,
        @JsonProperty("return_at") String returnAt,
        int transfers,
        @JsonProperty("return_transfers") int returnTransfers,
        int duration,
        @JsonProperty("duration_to") int durationTo,
        @JsonProperty("duration_back") int durationBack,
        String gate,
        String link) {}
