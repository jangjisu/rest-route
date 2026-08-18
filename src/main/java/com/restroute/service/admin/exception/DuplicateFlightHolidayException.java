package com.restroute.service.admin.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;
import java.time.LocalDate;

public class DuplicateFlightHolidayException extends BusinessException {

    public DuplicateFlightHolidayException(LocalDate holidayDate) {
        super(ResponseCode.INVALID_PARAMETER, "이미 등록된 날짜입니다: " + holidayDate);
    }

    public static DuplicateFlightHolidayException forDate(LocalDate holidayDate) {
        return new DuplicateFlightHolidayException(holidayDate);
    }
}
