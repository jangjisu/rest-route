package com.restroute.reststopcontent.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestThemeItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("테마휴게소 API 응답 필드를 item으로 매핑한다")
    void readValue_mapsRestThemeFields() throws Exception {
        String json = """
                {
                  "detail": "365일 꽃향기가 나는 휴게소입니다",
                  "pageNo": null,
                  "numOfRows": null,
                  "stdRestCd": "000001",
                  "stdRestNm": "서울만남(부산)휴게소",
                  "lsttmAltrUser": "SYSTEM",
                  "lsttmAltrDttm": "2026-07-28",
                  "svarAddr": "서울 서초구 원지동10-16",
                  "routeCd": "0010",
                  "routeNm": "경부선",
                  "itemNm": "4계절 꽃이 있는 휴게소",
                  "regId": "MANN003",
                  "regDtime": "07/12/2024 17:21:31."
                }
                """;

        RestThemeItem item = objectMapper.readValue(json, RestThemeItem.class);

        assertThat(item.getStdRestCd()).isEqualTo("000001");
        assertThat(item.getStdRestNm()).isEqualTo("서울만남(부산)휴게소");
        assertThat(item.getItemNm()).isEqualTo("4계절 꽃이 있는 휴게소");
        assertThat(item.getDetail()).isEqualTo("365일 꽃향기가 나는 휴게소입니다");
        assertThat(item.getRegId()).isEqualTo("MANN003");
        assertThat(item.getRegDtime()).isEqualTo("07/12/2024 17:21:31.");
        assertThat(item.getLsttmAltrUser()).isEqualTo("SYSTEM");
        assertThat(item.getLsttmAltrDttm()).isEqualTo("2026-07-28");
        assertThat(item.getSvarAddr()).isEqualTo("서울 서초구 원지동10-16");
        assertThat(item.getRouteCd()).isEqualTo("0010");
        assertThat(item.getRouteNm()).isEqualTo("경부선");
    }
}
