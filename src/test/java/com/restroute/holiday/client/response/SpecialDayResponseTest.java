package com.restroute.holiday.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpecialDayResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("getRestDeInfo 응답 JSON을 필드에 매핑한다")
    void readValue_mapsGetRestDeInfoFields() throws Exception {
        String json = """
                {
                  "response": {
                    "header": { "resultCode": "00", "resultMsg": "OK" },
                    "body": {
                      "numOfRows": 100,
                      "pageNo": 1,
                      "totalCount": 2,
                      "items": {
                        "item": [
                          { "locdate": 20260101, "seq": 1, "dateKind": "01", "isHoliday": "Y", "dateName": "1월1일" },
                          { "locdate": 20260815, "seq": 1, "dateKind": "01", "isHoliday": "Y", "dateName": "광복절" }
                        ]
                      }
                    }
                  }
                }
                """;

        SpecialDayResponse response = objectMapper.readValue(json, SpecialDayResponse.class);

        assertThat(response.response().header().isSuccess()).isTrue();
        assertThat(response.response().body().itemsOrEmpty()).hasSize(2);
        SpecialDayResponse.Item liberationDay =
                response.response().body().itemsOrEmpty().get(1);
        assertThat(liberationDay.locdate()).isEqualTo("20260815");
        assertThat(liberationDay.dateName()).isEqualTo("광복절");
        assertThat(liberationDay.isActualHoliday()).isTrue();
    }

    @Test
    @DisplayName("resultCode가 00이 아니면 isSuccess가 false다")
    void header_isSuccess_falseWhenResultCodeNotZeroZero() {
        SpecialDayResponse.Header header = new SpecialDayResponse.Header("30", "SERVICE_KEY_IS_NOT_REGISTERED_ERROR");

        assertThat(header.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("isHoliday가 Y가 아니면 isActualHoliday는 false다")
    void item_isActualHoliday_falseWhenNotY() {
        SpecialDayResponse.Item item = new SpecialDayResponse.Item("20260706", "제헌절", "N");

        assertThat(item.isActualHoliday()).isFalse();
    }

    @Test
    @DisplayName("itemsOrEmpty는 items나 item 리스트가 없으면 빈 리스트를 반환한다")
    void body_itemsOrEmpty_returnsEmptyListWhenMissing() {
        assertThat(new SpecialDayResponse.Body(null).itemsOrEmpty()).isEmpty();
        assertThat(new SpecialDayResponse.Body(new SpecialDayResponse.Items(null)).itemsOrEmpty())
                .isEmpty();
    }
}
