package com.restroute.reststopcontent.controller.response;

import com.restroute.reststopcontent.domain.RestFoodEntity;

public record FoodMenuItemResponse(
        String foodName,
        String foodCost,
        String description,
        boolean recommended,
        boolean bestFood,
        boolean premium,
        String season,
        String seasonLabel) {

    private static final String SEASON_ALL = "4";
    private static final String SEASON_SUMMER = "S";
    private static final String SEASON_WINTER = "W";

    public static FoodMenuItemResponse from(RestFoodEntity entity) {
        return new FoodMenuItemResponse(
                entity.getFoodName(),
                entity.getFoodCost(),
                entity.getDescription(),
                "Y".equals(entity.getRecommendYn()),
                "Y".equals(entity.getBestFoodYn()),
                "Y".equals(entity.getPremiumYn()),
                entity.getSeasonMenu(),
                seasonLabel(entity.getSeasonMenu()));
    }

    private static String seasonLabel(String season) {
        if (SEASON_ALL.equals(season)) {
            return "사계절";
        }
        if (SEASON_SUMMER.equals(season)) {
            return "여름";
        }
        if (SEASON_WINTER.equals(season)) {
            return "겨울";
        }
        return null;
    }
}
