package com.restroute.flight.controller.response;

import com.restroute.flight.domain.FlightCountryEntity;

public record FlightCountryResponse(String code, String korName, String engName) {

    public static FlightCountryResponse from(FlightCountryEntity entity) {
        return new FlightCountryResponse(entity.getCode(), entity.getKorName(), entity.getEngName());
    }
}
