package com.restroute.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.RestThemeItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestThemeEntityTest {

    @Test
    @DisplayName("테마휴게소 item을 엔티티로 매핑한다")
    void from_mapsRestThemeItemFields() throws Exception {
        RestThemeItem item = sampleItem("000001", "4계절 꽃이 있는 휴게소");

        RestThemeEntity entity = RestThemeEntity.from(item);

        assertThat(entity.getStdRestCd()).isEqualTo("000001");
        assertThat(entity.getStdRestNm()).isEqualTo("서울만남(부산)휴게소");
        assertThat(entity.getItemNm()).isEqualTo("4계절 꽃이 있는 휴게소");
        assertThat(entity.getDetail()).isEqualTo("365일 꽃향기가 나는 휴게소입니다");
        assertThat(entity.getRouteNm()).isEqualTo("경부선");
        assertThat(entity.getSvarAddr()).isEqualTo("서울 서초구 원지동10-16");
    }

    @Test
    @DisplayName("updateFrom은 상세 내용만 갱신하고 자연키(stdRestCd/itemNm)는 그대로 둔다")
    void updateFrom_updatesDetailFieldsOnly() throws Exception {
        RestThemeEntity entity = RestThemeEntity.from(sampleItem("000001", "4계절 꽃이 있는 휴게소"));

        entity.updateFrom(sampleItemWithDetail("000001", "4계절 꽃이 있는 휴게소", "새로 고친 설명"));

        assertThat(entity.getStdRestCd()).isEqualTo("000001");
        assertThat(entity.getItemNm()).isEqualTo("4계절 꽃이 있는 휴게소");
        assertThat(entity.getDetail()).isEqualTo("새로 고친 설명");
    }

    @Test
    @DisplayName("updateRestStopServiceAreaCode는 휴게소 코드를 연결한다")
    void updateRestStopServiceAreaCode_updatesLinkedCode() throws Exception {
        RestThemeEntity entity = RestThemeEntity.from(sampleItem("000001", "4계절 꽃이 있는 휴게소"));

        entity.updateRestStopServiceAreaCode("A00001");

        assertThat(entity.getRestStopServiceAreaCode()).isEqualTo("A00001");
    }

    private RestThemeItem sampleItem(String stdRestCd, String itemNm) throws Exception {
        return sampleItemWithDetail(stdRestCd, itemNm, "365일 꽃향기가 나는 휴게소입니다");
    }

    private RestThemeItem sampleItemWithDetail(String stdRestCd, String itemNm, String detail) throws Exception {
        String json = """
                {
                  "stdRestCd": "%s",
                  "stdRestNm": "서울만남(부산)휴게소",
                  "itemNm": "%s",
                  "detail": "%s",
                  "routeCd": "0010",
                  "routeNm": "경부선",
                  "svarAddr": "서울 서초구 원지동10-16"
                }
                """.formatted(stdRestCd, itemNm, detail);
        return new ObjectMapper().readValue(json, RestThemeItem.class);
    }
}
