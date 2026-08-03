package com.restroute.service.image.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class RestStopNotFoundException extends BusinessException {

    public RestStopNotFoundException(String serviceAreaCode) {
        super(ResponseCode.NOT_FOUND, "Rest stop not found: " + serviceAreaCode);
    }

    public static RestStopNotFoundException forServiceAreaCode(String serviceAreaCode) {
        return new RestStopNotFoundException(serviceAreaCode);
    }
}
