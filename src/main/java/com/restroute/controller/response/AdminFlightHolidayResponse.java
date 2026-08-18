package com.restroute.controller.response;

import com.restroute.holiday.domain.HolidayEntity;
import java.time.format.DateTimeFormatter;

public record AdminFlightHolidayResponse(Long id, String date, String name) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static AdminFlightHolidayResponse from(HolidayEntity entity) {
        return new AdminFlightHolidayResponse(
                entity.getId(), DATE_FORMAT.format(entity.getHolidayDate()), entity.getName());
    }
}
