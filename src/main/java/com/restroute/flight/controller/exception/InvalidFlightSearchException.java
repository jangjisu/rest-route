package com.restroute.flight.controller.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;
import java.time.LocalDate;

public class InvalidFlightSearchException extends BusinessException {

    public InvalidFlightSearchException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public static InvalidFlightSearchException forReversedDateRange(LocalDate dateFrom, LocalDate dateTo) {
        return new InvalidFlightSearchException("종료일(" + dateTo + ")은 시작일(" + dateFrom + ")보다 빠를 수 없습니다.");
    }

    public static InvalidFlightSearchException forDateRangeTooWide(
            LocalDate dateFrom, LocalDate dateTo, int maxMonths) {
        return new InvalidFlightSearchException(
                "날짜 범위는 최대 " + maxMonths + "개월을 넘을 수 없습니다: " + dateFrom + " ~ " + dateTo);
    }

    public static InvalidFlightSearchException forEmptyNights() {
        return new InvalidFlightSearchException("여행 박수(nights)는 최소 1개 이상 선택해야 합니다.");
    }

    public static InvalidFlightSearchException forUnknownRegion(String region) {
        return new InvalidFlightSearchException("알 수 없는 지역권입니다: " + region);
    }
}
