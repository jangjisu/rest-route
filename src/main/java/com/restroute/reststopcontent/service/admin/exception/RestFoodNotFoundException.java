package com.restroute.reststopcontent.service.admin.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class RestFoodNotFoundException extends BusinessException {

    public RestFoodNotFoundException(Long foodId) {
        super(ResponseCode.NOT_FOUND, "Rest food not found: " + foodId);
    }

    public static RestFoodNotFoundException forId(Long foodId) {
        return new RestFoodNotFoundException(foodId);
    }
}
