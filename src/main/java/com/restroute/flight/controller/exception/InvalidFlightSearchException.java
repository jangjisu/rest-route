package com.restroute.flight.controller.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;
import java.time.LocalDate;

public class InvalidFlightSearchException extends BusinessException {

    public InvalidFlightSearchException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public static InvalidFlightSearchException forReversedDateRange(LocalDate dateFrom, LocalDate dateTo) {
        return new InvalidFlightSearchException(
                "dateTo must not be before dateFrom: dateFrom=" + dateFrom + ", dateTo=" + dateTo);
    }

    public static InvalidFlightSearchException forDateRangeTooWide(
            LocalDate dateFrom, LocalDate dateTo, int maxMonths) {
        return new InvalidFlightSearchException(
                "Date range must not exceed " + maxMonths + " months: dateFrom=" + dateFrom + ", dateTo=" + dateTo);
    }

    public static InvalidFlightSearchException forEmptyNights() {
        return new InvalidFlightSearchException("nights must contain at least one value");
    }

    public static InvalidFlightSearchException forUnknownRegion(String region) {
        return new InvalidFlightSearchException("Unknown region: " + region);
    }
}
