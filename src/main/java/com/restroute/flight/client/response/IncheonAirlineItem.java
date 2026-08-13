package com.restroute.flight.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IncheonAirlineItem(
        @JsonProperty("airlineIata") String iataCode,
        @JsonProperty("airlineName") String name) {}
