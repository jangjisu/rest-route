package com.restroute.flight.controller.exception;

import com.restroute.flight.controller.response.FlightApiError;
import java.util.List;

public class InvalidFlightSearchException extends RuntimeException {

    private final List<FlightApiError.Detail> details;

    public InvalidFlightSearchException(List<FlightApiError.Detail> details) {
        super(details.size() + " fields are invalid");
        this.details = details;
    }

    public List<FlightApiError.Detail> details() {
        return details;
    }
}
