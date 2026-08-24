package com.restroute.reststop.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("JSON xValue, yValue 필드를 휴게소 item 좌표로 매핑한다")
    void deserialize_mapsCoordinateFields() throws Exception {
        String json = """
                {
                  "unitCode": "001",
                  "unitName": "서울만남(부산)휴게소",
                  "routeNo": "0010",
                  "routeName": "경부선",
                  "xValue": "127.104397",
                  "yValue": "37.332583",
                  "stdRestCd": "000001",
                  "serviceAreaCode": "A00001"
                }
                """;

        RestStopItem item = objectMapper.readValue(json, RestStopItem.class);

        assertThat(item.getUnitCode()).isEqualTo("001");
        assertThat(item.getUnitName()).isEqualTo("서울만남(부산)휴게소");
        assertThat(item.getXValue()).isEqualTo("127.104397");
        assertThat(item.getYValue()).isEqualTo("37.332583");
    }
}
