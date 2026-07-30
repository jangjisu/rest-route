package com.restroute.service.admin;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class InvalidRestFoodEditException extends BusinessException {

    public InvalidRestFoodEditException(String message) {
        super(ResponseCode.INVALID_PARAMETER, message);
    }

    public static InvalidRestFoodEditException forSyncedFoodDeletion(Long foodId) {
        return new InvalidRestFoodEditException("Cannot delete a synced rest food row: " + foodId);
    }
}
