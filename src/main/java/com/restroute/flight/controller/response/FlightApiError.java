package com.restroute.flight.controller.response;

import java.util.List;

public record FlightApiError(String code, String message, List<Detail> details) {

    public static FlightApiError of(String code, String message) {
        return new FlightApiError(code, message, null);
    }

    public record Detail(String field, String code) {}
}
