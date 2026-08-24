package com.restroute.controller.response;

import com.restroute.domain.RestFoodEntity;
import java.util.ArrayList;
import java.util.List;

public record FoodMenuResponse(List<FoodMenuItemResponse> menus, List<FoodMenuSectionResponse> sections) {

    public static FoodMenuResponse from(List<RestFoodEntity> foods) {
        List<FoodMenuItemResponse> menus =
                foods.stream().map(FoodMenuItemResponse::from).toList();
        return new FoodMenuResponse(menus, sectionsFrom(menus));
    }

    private static List<FoodMenuSectionResponse> sectionsFrom(List<FoodMenuItemResponse> menus) {
        List<FoodMenuSectionResponse> sections = new ArrayList<>();
        addSection(
                sections,
                "recommended",
                "추천 메뉴",
                menus.stream()
                        .filter(menu -> menu.recommended() || menu.bestFood())
                        .toList());
        addSection(
                sections,
                "premium",
                "프리미엄",
                menus.stream().filter(FoodMenuItemResponse::premium).toList());
        addSection(
                sections,
                "seasonal",
                "계절 메뉴",
                menus.stream().filter(FoodMenuResponse::isSeasonal).toList());
        return List.copyOf(sections);
    }

    private static boolean isSeasonal(FoodMenuItemResponse menu) {
        return "S".equals(menu.season()) || "W".equals(menu.season());
    }

    private static void addSection(
            List<FoodMenuSectionResponse> sections, String key, String title, List<FoodMenuItemResponse> menus) {
        if (!menus.isEmpty()) {
            sections.add(new FoodMenuSectionResponse(key, title, menus));
        }
    }
}
