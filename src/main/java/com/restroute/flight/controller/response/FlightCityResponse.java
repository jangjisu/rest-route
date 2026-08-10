package com.restroute.flight.controller.response;

import com.restroute.flight.domain.FlightCityEntity;

public record FlightCityResponse(String code, String name, String countryCode) {

    public static FlightCityResponse from(FlightCityEntity entity) {
        return new FlightCityResponse(entity.getCode(), entity.getName(), entity.getCountryCode());
    }
}
