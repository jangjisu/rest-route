package com.restroute.service.admin.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class FlightHolidayNotFoundException extends BusinessException {

    public FlightHolidayNotFoundException(Long holidayId) {
        super(ResponseCode.NOT_FOUND, "Flight holiday not found: " + holidayId);
    }

    public static FlightHolidayNotFoundException forId(Long holidayId) {
        return new FlightHolidayNotFoundException(holidayId);
    }
}
