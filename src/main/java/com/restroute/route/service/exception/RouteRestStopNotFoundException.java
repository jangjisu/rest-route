package com.restroute.route.service.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class RouteRestStopNotFoundException extends BusinessException {

    public RouteRestStopNotFoundException(String message) {
        super(ResponseCode.NOT_FOUND, message);
    }
}
