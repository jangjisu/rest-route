package com.restroute.oilprice.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestOilItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("주유소 편의시설 API 응답 필드를 역직렬화한다")
    void readValue_mapsRestOilFields() throws Exception {
        String json = """
                {
                  "stdRestCd": "000002",
                  "stdRestNm": "서울만남(부산)주유소",
                  "stime": "00:00",
                  "etime": "24:00",
                  "redId": "MANJ03",
                  "redDtime": "16/03/10",
                  "lsttmAltrUser": "SYSTEM",
                  "lsttmAltrDttm": "2026-06-15",
                  "svarAddr": "서울시 서초구 원지동10-16",
                  "routeCd": "0010",
                  "routeNm": "경부선",
                  "psCode": "07",
                  "psName": "쉼터",
                  "psDesc": "고객쉼터"
                }
                """;

        RestOilItem item = objectMapper.readValue(json, RestOilItem.class);

        assertThat(item.getStandardRestCode()).isEqualTo("000002");
        assertThat(item.getStandardRestName()).isEqualTo("서울만남(부산)주유소");
        assertThat(item.getStartTime()).isEqualTo("00:00");
        assertThat(item.getEndTime()).isEqualTo("24:00");
        assertThat(item.getOriginalModifierId()).isEqualTo("MANJ03");
        assertThat(item.getOriginalModifiedDateTime()).isEqualTo("16/03/10");
        assertThat(item.getLastModifiedUser()).isEqualTo("SYSTEM");
        assertThat(item.getLastModifiedDateTime()).isEqualTo("2026-06-15");
        assertThat(item.getServiceAreaAddress()).isEqualTo("서울시 서초구 원지동10-16");
        assertThat(item.getRouteCode()).isEqualTo("0010");
        assertThat(item.getRouteName()).isEqualTo("경부선");
        assertThat(item.getConvenienceCode()).isEqualTo("07");
        assertThat(item.getConvenienceName()).isEqualTo("쉼터");
        assertThat(item.getConvenienceDescription()).isEqualTo("고객쉼터");
    }

    @Test
    @DisplayName("실측 응답의 nullable 필드를 허용한다")
    void readValue_allowsNullableFields() throws Exception {
        String json = """
                {
                  "routeNm": null,
                  "psDesc": null
                }
                """;

        RestOilItem item = objectMapper.readValue(json, RestOilItem.class);

        assertThat(item.getRouteName()).isNull();
        assertThat(item.getConvenienceDescription()).isNull();
    }
}
