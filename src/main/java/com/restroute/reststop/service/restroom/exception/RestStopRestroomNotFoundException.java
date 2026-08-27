package com.restroute.reststop.service.restroom.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class RestStopRestroomNotFoundException extends BusinessException {

    private RestStopRestroomNotFoundException(String message) {
        super(ResponseCode.NOT_FOUND, message);
    }

    public static RestStopRestroomNotFoundException forId(Long id) {
        return new RestStopRestroomNotFoundException("화장실 현황(id=" + id + ")을 찾을 수 없습니다.");
    }
}
