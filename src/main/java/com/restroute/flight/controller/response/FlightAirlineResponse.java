package com.restroute.flight.controller.response;

import com.restroute.flight.domain.FlightAirlineEntity;

public record FlightAirlineResponse(String code, String korName, String engName) {

    public static FlightAirlineResponse from(FlightAirlineEntity entity) {
        return new FlightAirlineResponse(entity.getCode(), entity.getKorName(), entity.getEngName());
    }
}
