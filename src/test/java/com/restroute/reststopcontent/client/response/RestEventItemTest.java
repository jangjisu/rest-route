package com.restroute.reststopcontent.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestEventItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("휴게소 이벤트 API 응답 필드를 item으로 매핑한다")
    void readValue_mapsRestEventFields() throws Exception {
        String json = """
                {
                  "pageNo": null,
                  "numOfRows": null,
                  "stdRestCd": "000001",
                  "stime": "2020-01-01",
                  "routeCd": "0010",
                  "svarAddr": "서울 서초구 원지동10-16",
                  "routeNm": "경부선",
                  "stdRestNm": "서울만남(부산)휴게소",
                  "etime": "2027-12-31",
                  "lsttmAltrUser": "SYSTEM",
                  "lsttmAltrDttm": "2026-07-28",
                  "lastId": "dmsrud527",
                  "lastDtime": "20260621150549",
                  "stdRestGubun": "S",
                  "eventSeq": "1665",
                  "eventDetail": "우리 휴게소에서 한식당 식사시 TEN+1 이벤트를 이용하실 수 있습니다.",
                  "eventNm": "TEN+1 이벤트"
                }
                """;

        RestEventItem item = objectMapper.readValue(json, RestEventItem.class);

        assertThat(item.getStdRestCd()).isEqualTo("000001");
        assertThat(item.getEventSeq()).isEqualTo("1665");
        assertThat(item.getEventNm()).isEqualTo("TEN+1 이벤트");
        assertThat(item.getEventDetail()).isEqualTo("우리 휴게소에서 한식당 식사시 TEN+1 이벤트를 이용하실 수 있습니다.");
        assertThat(item.getStime()).isEqualTo("2020-01-01");
        assertThat(item.getEtime()).isEqualTo("2027-12-31");
        assertThat(item.getStdRestNm()).isEqualTo("서울만남(부산)휴게소");
        assertThat(item.getSvarAddr()).isEqualTo("서울 서초구 원지동10-16");
        assertThat(item.getRouteCd()).isEqualTo("0010");
        assertThat(item.getRouteNm()).isEqualTo("경부선");
        assertThat(item.getStdRestGubun()).isEqualTo("S");
        assertThat(item.getLastId()).isEqualTo("dmsrud527");
        assertThat(item.getLastDtime()).isEqualTo("20260621150549");
        assertThat(item.getLsttmAltrUser()).isEqualTo("SYSTEM");
        assertThat(item.getLsttmAltrDttm()).isEqualTo("2026-07-28");
    }
}
