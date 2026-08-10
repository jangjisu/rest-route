package com.restroute.flight.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelpayoutsCityItem(
        String name,
        String code,
        @JsonProperty("country_code") String countryCode,
        @JsonProperty("has_flightable_airport") boolean hasFlightableAirport) {}
