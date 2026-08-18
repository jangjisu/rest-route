package com.restroute.service.admin.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;
import java.time.LocalDate;

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

    /** 주말은 이미 무조건 비근무일이라 공휴일로 따로 등록할 필요가 없다. */
    public static InvalidFlightHolidayRequestException weekendNotAllowed(LocalDate date) {
        return new InvalidFlightHolidayRequestException("주말은 공휴일로 등록할 수 없습니다: " + date);
    }
}
