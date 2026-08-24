package com.restroute.placesearch.controller.response;

public record PlaceCandidateResponse(String name, String address, double latitude, double longitude) {

    public static PlaceCandidateResponse of(String name, String address, double latitude, double longitude) {
        return new PlaceCandidateResponse(name, address, latitude, longitude);
    }
}
