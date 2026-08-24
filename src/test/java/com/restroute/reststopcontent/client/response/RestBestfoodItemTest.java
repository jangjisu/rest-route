package com.restroute.reststopcontent.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestBestfoodItemTest {

    @Test
    @DisplayName("restBestfoodList list 항목을 원본 문자열 필드로 매핑한다")
    void readValue_mapsRestBestfoodFields() throws Exception {
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

        assertThat(item.getStdRestCd()).isEqualTo("000001");
        assertThat(item.getStdRestNm()).isEqualTo("서울만남(부산)휴게소");
        assertThat(item.getRestCd()).isEqualTo("S000001");
        assertThat(item.getRouteCd()).isEqualTo("0010");
        assertThat(item.getRouteNm()).isEqualTo("경부선");
        assertThat(item.getServiceAreaAddress()).isEqualTo("서울 서초구 원지동10-16");
        assertThat(item.getSeq()).isEqualTo("272");
        assertThat(item.getFoodNm()).isEqualTo("농심어묵우동");
        assertThat(item.getFoodCost()).isEqualTo("7000");
        assertThat(item.getEtc()).isEqualTo("부산어묵꼬치를 첨가한 우동.");
        assertThat(item.getFoodMaterial()).isEqualTo("냉동면 1ea");
        assertThat(item.getRecommendyn()).isEqualTo("Y");
        assertThat(item.getBestfoodyn()).isEqualTo("N");
        assertThat(item.getPremiumyn()).isEqualTo("N");
        assertThat(item.getSeasonMenu()).isEqualTo("4");
        assertThat(item.getApp()).isEqualTo("Y");
    }
}
