package com.restroute.reststopcontent.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.reststopcontent.client.response.RestBestfoodItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestFoodEntityTest {

    @Test
    @DisplayName("음식 메뉴 item을 원본 문자열 그대로 엔티티로 매핑한다")
    void from_mapsRestBestfoodItemFields() throws Exception {
        String json = """
                {
                  "stdRestCd": "000001",
                  "stdRestNm": "서울만남(부산)휴게소",
                  "restCd": "S000001",
                  "routeCd": "0010",
                  "routeNm": "경부선",
                  "svarAddr": "서울 서초구 원지동10-16",
                  "seq": "272",
                  "foodNm": "농심어묵우동",
                  "foodCost": "7000",
                  "etc": "부산어묵꼬치를 첨가한 우동.",
                  "foodMaterial": "냉동면 1ea",
                  "recommendyn": "Y",
                  "bestfoodyn": "N",
                  "premiumyn": "N",
                  "seasonMenu": "4",
                  "app": "Y"
                }
                """;
        RestBestfoodItem item = new ObjectMapper().readValue(json, RestBestfoodItem.class);

        RestFoodEntity entity = RestFoodEntity.from(item);

        assertThat(entity.getStdRestCd()).isEqualTo("000001");
        assertThat(entity.getStdRestName()).isEqualTo("서울만남(부산)휴게소");
        assertThat(entity.getRestCode()).isEqualTo("S000001");
        assertThat(entity.getRouteCode()).isEqualTo("0010");
        assertThat(entity.getRouteName()).isEqualTo("경부선");
        assertThat(entity.getServiceAreaAddress()).isEqualTo("서울 서초구 원지동10-16");
        assertThat(entity.getSeq()).isEqualTo("272");
        assertThat(entity.getFoodName()).isEqualTo("농심어묵우동");
        assertThat(entity.getFoodCost()).isEqualTo("7000");
        assertThat(entity.getDescription()).isEqualTo("부산어묵꼬치를 첨가한 우동.");
        assertThat(entity.getFoodMaterial()).isEqualTo("냉동면 1ea");
        assertThat(entity.getRecommendYn()).isEqualTo("Y");
        assertThat(entity.getBestFoodYn()).isEqualTo("N");
        assertThat(entity.getPremiumYn()).isEqualTo("N");
        assertThat(entity.getSeasonMenu()).isEqualTo("4");
        assertThat(entity.getAppExposeYn()).isEqualTo("Y");
    }

    @Test
    @DisplayName("applyAdminEdit는 메뉴 필드를 갱신하고 잠금 플래그를 켠다")
    void applyAdminEdit_updatesFieldsAndSetsOverrideFlag() throws Exception {
        RestFoodEntity entity = RestFoodEntity.from(sampleItem());

        entity.applyAdminEdit("수정된메뉴", "9000", "수정된 설명");

        assertThat(entity.getFoodName()).isEqualTo("수정된메뉴");
        assertThat(entity.getFoodCost()).isEqualTo("9000");
        assertThat(entity.getDescription()).isEqualTo("수정된 설명");
        assertThat(entity.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("clearAdminOverride를 호출하면 잠금 플래그가 꺼진다")
    void clearAdminOverride_resetsOverrideFlag() throws Exception {
        RestFoodEntity entity = RestFoodEntity.from(sampleItem());
        entity.applyAdminEdit("수정된메뉴", "9000", "수정된 설명");

        entity.clearAdminOverride();

        assertThat(entity.isAdminOverridden()).isFalse();
    }

    @Test
    @DisplayName("createByAdmin은 처음부터 잠긴 상태로, 외부 API와 겹치지 않는 seq로 새 메뉴를 만든다")
    void createByAdmin_startsOverriddenWithGeneratedSeq() {
        RestFoodEntity entity = RestFoodEntity.createByAdmin("A00001", "000001", "커스텀메뉴", "5000", "직접 추가한 메뉴");

        assertThat(entity.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(entity.getStdRestCd()).isEqualTo("000001");
        assertThat(entity.getFoodName()).isEqualTo("커스텀메뉴");
        assertThat(entity.getFoodCost()).isEqualTo("5000");
        assertThat(entity.getDescription()).isEqualTo("직접 추가한 메뉴");
        assertThat(entity.getSeq()).startsWith("ADMIN-");
        assertThat(entity.isAdminOverridden()).isTrue();
        assertThat(entity.isAdminCreated()).isTrue();
    }

    @Test
    @DisplayName("동기화로 만들어진 메뉴는 관리자가 수정해도 isAdminCreated는 false다")
    void isAdminCreated_isFalseForSyncedRowEvenAfterAdminEdit() throws Exception {
        RestFoodEntity entity = RestFoodEntity.from(sampleItem());

        entity.applyAdminEdit("수정된메뉴", "9000", "수정된 설명");

        assertThat(entity.isAdminCreated()).isFalse();
    }

    @Test
    @DisplayName("seq가 없는 메뉴는 isAdminCreated가 false다")
    void isAdminCreated_isFalseWhenSeqIsNull() throws Exception {
        String json = """
                {
                  "stdRestCd": "000001",
                  "foodNm": "농심어묵우동",
                  "foodCost": "7000",
                  "etc": "부산어묵꼬치를 첨가한 우동."
                }
                """;
        RestFoodEntity entity = RestFoodEntity.from(new ObjectMapper().readValue(json, RestBestfoodItem.class));

        assertThat(entity.getSeq()).isNull();
        assertThat(entity.isAdminCreated()).isFalse();
    }

    private RestBestfoodItem sampleItem() throws Exception {
        String json = """
                {
                  "stdRestCd": "000001",
                  "seq": "272",
                  "foodNm": "농심어묵우동",
                  "foodCost": "7000",
                  "etc": "부산어묵꼬치를 첨가한 우동."
                }
                """;
        return new ObjectMapper().readValue(json, RestBestfoodItem.class);
    }
}
