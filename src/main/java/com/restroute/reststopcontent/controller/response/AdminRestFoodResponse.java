package com.restroute.reststopcontent.controller.response;

import com.restroute.reststopcontent.domain.RestFoodEntity;

public record AdminRestFoodResponse(
        Long id,
        String foodName,
        String foodCost,
        String description,
        boolean adminOverridden,
        boolean adminCreated,
        boolean hasImage) {

    public static AdminRestFoodResponse from(RestFoodEntity entity, boolean hasImage) {
        return new AdminRestFoodResponse(
                entity.getId(),
                entity.getFoodName(),
                entity.getFoodCost(),
                entity.getDescription(),
                entity.isAdminOverridden(),
                entity.isAdminCreated(),
                hasImage);
    }
}
