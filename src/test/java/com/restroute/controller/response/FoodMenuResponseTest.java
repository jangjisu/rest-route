package com.restroute.controller.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.RestBestfoodItem;
import com.restroute.domain.RestFoodEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FoodMenuResponseTest {

    @Test
    @DisplayName("음식 메뉴 구분 값을 화면용 필드로 변환한다")
    void from_mapsMenuKinds() throws Exception {
        RestFoodEntity highlighted = foodEntity("농심어묵우동", "7000", "시원한 우동", "Y", "Y", "Y", "S");
        RestFoodEntity normal = foodEntity("한우국밥", "9000", "든든한 국밥", "N", "N", "N", "4");

        FoodMenuResponse response = FoodMenuResponse.from(List.of(highlighted, normal));

        assertThat(response.menus())
                .extracting(
                        "foodName",
                        "foodCost",
                        "description",
                        "recommended",
                        "bestFood",
                        "premium",
                        "season",
                        "seasonLabel")
                .containsExactly(
                        tuple("농심어묵우동", "7000", "시원한 우동", true, true, true, "S", "여름"),
                        tuple("한우국밥", "9000", "든든한 국밥", false, false, false, "4", "사계절"));
    }

    @Test
    @DisplayName("음식 메뉴를 추천 섹션별로 묶는다")
    void from_groupsMenusByRecommendationSection() throws Exception {
        RestFoodEntity representative = foodEntity("농심어묵우동", "7000", "시원한 우동", "Y", "N", "N", "4");
        RestFoodEntity best = foodEntity("한우국밥", "9000", "든든한 국밥", "N", "Y", "N", "4");
        RestFoodEntity premium = foodEntity("한우스테이크", "15000", "프리미엄 메뉴", "N", "N", "Y", "4");
        RestFoodEntity seasonal = foodEntity("초계국수", "8000", "여름 메뉴", "N", "N", "N", "S");
        RestFoodEntity winter = foodEntity("굴국밥", "8500", "겨울 메뉴", "N", "N", "N", "W");

        FoodMenuResponse response = FoodMenuResponse.from(List.of(representative, best, premium, seasonal, winter));

        assertThat(response.sections())
                .extracting("key", "title")
                .containsExactly(tuple("recommended", "추천 메뉴"), tuple("premium", "프리미엄"), tuple("seasonal", "계절 메뉴"));
        assertThat(response.sections().get(0).menus()).extracting("foodName").containsExactly("농심어묵우동", "한우국밥");
        assertThat(response.sections().get(1).menus()).extracting("foodName").containsExactly("한우스테이크");
        assertThat(response.sections().get(2).menus()).extracting("foodName").containsExactly("초계국수", "굴국밥");
    }

    @Test
    @DisplayName("음식 메뉴가 없으면 빈 목록을 반환한다")
    void from_returnsEmptyWhenNoFoods() {
        FoodMenuResponse response = FoodMenuResponse.from(List.of());

        assertThat(response.menus()).isEmpty();
        assertThat(response.sections()).isEmpty();
    }

    private RestFoodEntity foodEntity(
            String foodNm,
            String foodCost,
            String etc,
            String recommendYn,
            String bestFoodYn,
            String premiumYn,
            String seasonMenu)
            throws Exception {
        String json = """
                {
                  "foodNm": "%s",
                  "foodCost": "%s",
                  "etc": "%s",
                  "recommendyn": "%s",
                  "bestfoodyn": "%s",
                  "premiumyn": "%s",
                  "seasonMenu": "%s"
                }
                """.formatted(foodNm, foodCost, etc, recommendYn, bestFoodYn, premiumYn, seasonMenu);
        return RestFoodEntity.from(new ObjectMapper().readValue(json, RestBestfoodItem.class));
    }
}
