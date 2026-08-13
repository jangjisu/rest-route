package com.restroute.flight.controller.response;

import com.restroute.flight.domain.FlightAirportEntity;

public record FlightAirportResponse(String code, String korName, String engName, String cityCode, String countryCode) {

    public static FlightAirportResponse from(FlightAirportEntity entity) {
        return new FlightAirportResponse(
                entity.getCode(),
                entity.getKorName(),
                entity.getEngName(),
                entity.getCityCode(),
                entity.getCountryCode());
    }
}
