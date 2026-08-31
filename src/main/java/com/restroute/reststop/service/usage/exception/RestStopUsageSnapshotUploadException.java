package com.restroute.reststop.service.usage.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class RestStopUsageSnapshotUploadException extends BusinessException {

    public RestStopUsageSnapshotUploadException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public RestStopUsageSnapshotUploadException(String message, Throwable cause) {
        super(ResponseCode.INVALID_PARAMETER, message, cause);
    }
}
