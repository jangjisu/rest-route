package com.restroute.reststop.service.image.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class InvalidRestStopImageException extends BusinessException {

    public InvalidRestStopImageException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public InvalidRestStopImageException(String message, Throwable cause) {
        super(ResponseCode.INVALID_PARAMETER, message, cause);
    }
}
