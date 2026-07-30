package com.restroute.service.admin;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class InvalidRestStopEditException extends BusinessException {

    public InvalidRestStopEditException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public static InvalidRestStopEditException forInvalidCoordinate(String value) {
        return new InvalidRestStopEditException("Invalid coordinate value: " + value);
    }
}
