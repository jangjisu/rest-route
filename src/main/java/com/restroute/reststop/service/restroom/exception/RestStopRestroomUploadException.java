package com.restroute.reststop.service.restroom.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class RestStopRestroomUploadException extends BusinessException {

    public RestStopRestroomUploadException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public RestStopRestroomUploadException(String message, Throwable cause) {
        super(ResponseCode.INVALID_PARAMETER, message, cause);
    }
}
