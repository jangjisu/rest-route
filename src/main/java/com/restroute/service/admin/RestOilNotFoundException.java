package com.restroute.service.admin;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class RestOilNotFoundException extends BusinessException {

    private RestOilNotFoundException(String message) {
        super(ResponseCode.NOT_FOUND, message);
    }

    public static RestOilNotFoundException forId(Long oilId) {
        return new RestOilNotFoundException("주유소를 찾을 수 없습니다: " + oilId);
    }
}
