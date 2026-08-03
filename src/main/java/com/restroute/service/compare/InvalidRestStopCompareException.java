package com.restroute.service.compare;

public class InvalidRestStopCompareException extends RuntimeException {

    private InvalidRestStopCompareException(String message) {
        super(message);
    }

    public static InvalidRestStopCompareException forSameServiceAreaCode(String serviceAreaCode) {
        return new InvalidRestStopCompareException("같은 휴게소는 비교할 수 없습니다: " + serviceAreaCode);
    }
}
