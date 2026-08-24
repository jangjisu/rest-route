package com.restroute.reststop.service.salesranking.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class SalesRankingUploadException extends BusinessException {

    public static SalesRankingUploadException of(String message) {
        return new SalesRankingUploadException(message);
    }

    public SalesRankingUploadException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public SalesRankingUploadException(String message, Throwable cause) {
        super(ResponseCode.INVALID_PARAMETER, message, cause);
    }
}
