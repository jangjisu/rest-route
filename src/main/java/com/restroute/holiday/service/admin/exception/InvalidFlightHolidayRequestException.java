package com.restroute.holiday.service.admin.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class InvalidFlightHolidayRequestException extends BusinessException {

    public InvalidFlightHolidayRequestException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public InvalidFlightHolidayRequestException(String message, Throwable cause) {
        super(ResponseCode.INVALID_PARAMETER, message, cause);
    }

    public static InvalidFlightHolidayRequestException blankDate() {
        return new InvalidFlightHolidayRequestException("date is required");
    }

    public static InvalidFlightHolidayRequestException invalidDate(String rawDate, Throwable cause) {
        return new InvalidFlightHolidayRequestException("Invalid date format: " + rawDate, cause);
    }

    public static InvalidFlightHolidayRequestException blankName() {
        return new InvalidFlightHolidayRequestException("name is required");
    }
}
