package com.restroute.reststopcontent.controller.response;

import java.util.List;

public record FoodMenuSectionResponse(String key, String title, List<FoodMenuItemResponse> menus) {}
