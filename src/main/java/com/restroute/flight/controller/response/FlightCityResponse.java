package com.restroute.flight.controller.response;

import com.restroute.flight.domain.FlightCityEntity;

public record FlightCityResponse(
        String code, String name, String nameKo, String countryCode, String countryName, String regionGroup) {

    public static FlightCityResponse from(FlightCityEntity entity) {
        return new FlightCityResponse(
                entity.getCode(),
                entity.getName(),
                entity.getNameKo(),
                entity.getCountryCode(),
                entity.getCountryName(),
                entity.getRegionGroup());
    }
}
