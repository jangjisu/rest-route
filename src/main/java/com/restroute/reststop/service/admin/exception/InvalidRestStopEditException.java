package com.restroute.reststop.service.admin.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class InvalidRestStopEditException extends BusinessException {

    public InvalidRestStopEditException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public InvalidRestStopEditException(String message, Throwable cause) {
        super(ResponseCode.INVALID_PARAMETER, message, cause);
    }

    public static InvalidRestStopEditException forInvalidCoordinate(String value, Throwable cause) {
        return new InvalidRestStopEditException("Invalid coordinate value: " + value, cause);
    }
}
