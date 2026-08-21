package com.restroute.flight.controller.response;

import com.restroute.holiday.domain.HolidayEntity;
import java.time.format.DateTimeFormatter;

public record FlightHolidayResponse(String date, String name) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static FlightHolidayResponse from(HolidayEntity entity) {
        return new FlightHolidayResponse(DATE_FORMAT.format(entity.getHolidayDate()), entity.getName());
    }
}
