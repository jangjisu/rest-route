package com.restroute.evcharger.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvChargerResponseTest {

    @Test
    @DisplayName("전기차 충전소 JSON 응답의 메타데이터와 충전기 목록을 역직렬화한다")
    void response_deserializesJsonPayload() throws Exception {
        String json = """
                {
                  "resultCode": "00",
                  "resultMsg": "NORMAL SERVICE.",
                  "totalCount": 201,
                  "pageNo": 1,
                  "numOfRows": 200,
                  "items": {
                    "item": [{
                      "statNm": "서울만남(부산) 휴게소",
                      "statId": "ME178009",
                      "chgerId": "01",
                      "lat": "37.4600218",
                      "lng": "127.0420378",
                      "kind": "C0",
                      "kindDetail": "C001",
                      "delYn": "N",
                      "maker": "채비"
                    }]
                  }
                }
                """;

        EvChargerResponse response = new ObjectMapper().readValue(json, EvChargerResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTotalPageCount()).isEqualTo(2);
        assertThat(response.getList()).singleElement().satisfies(item -> {
            assertThat(item.getStatId()).isEqualTo("ME178009");
            assertThat(item.getChgerId()).isEqualTo("01");
            assertThat(item.getLat()).isEqualTo("37.4600218");
            assertThat(item.getLng()).isEqualTo("127.0420378");
            assertThat(item.getMaker()).isEqualTo("채비");
        });
    }

    @Test
    @DisplayName("items가 없으면 빈 목록을 반환한다")
    void response_returnsEmptyListWhenItemsAreMissing() throws Exception {
        EvChargerResponse response = new ObjectMapper()
                .readValue(
                        "{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL SERVICE.\",\"totalCount\":0,\"numOfRows\":200}",
                        EvChargerResponse.class);

        assertThat(response.getList()).isEmpty();
        assertThat(response.getTotalPageCount()).isEqualTo(1);
    }
}
