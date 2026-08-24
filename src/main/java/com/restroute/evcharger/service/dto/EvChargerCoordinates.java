package com.restroute.evcharger.service.dto;

public record EvChargerCoordinates(double latitude, double longitude) {

    public static EvChargerCoordinates of(double latitude, double longitude) {
        return new EvChargerCoordinates(latitude, longitude);
    }
}
