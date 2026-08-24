package com.restroute.oilprice.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpinetAverageOilPriceResponseTest {

    @Test
    @DisplayName("오피넷 전국 평균가 응답의 RESULT.OIL 배열을 파싱한다")
    void parseAverageOilPriceResponse() throws Exception {
        String json = """
                {
                  "RESULT": {
                    "OIL": [
                      {
                        "TRADE_DT": "20260707",
                        "PRODCD": "B027",
                        "PRODNM": "휘발유",
                        "PRICE": "1892.88",
                        "DIFF": "-4.19"
                      }
                    ]
                  }
                }
                """;

        OpinetAverageOilPriceResponse response =
                new ObjectMapper().readValue(json, OpinetAverageOilPriceResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getErrorMessage()).isEmpty();
        assertThat(response.oil()).hasSize(1);
        OpinetAverageOilPriceItem item = response.oil().get(0);
        assertThat(item.getTradeDate()).isEqualTo("20260707");
        assertThat(item.getProductCode()).isEqualTo("B027");
        assertThat(item.getProductName()).isEqualTo("휘발유");
        assertThat(item.getPrice()).isEqualTo("1892.88");
        assertThat(item.getDiff()).isEqualTo("-4.19");
    }

    @Test
    @DisplayName("RESULT.OIL 배열이 없으면 실패 응답으로 판단한다")
    void missingOil_isNotSuccess() throws Exception {
        OpinetAverageOilPriceResponse response =
                new ObjectMapper().readValue("{\"RESULT\":{}}", OpinetAverageOilPriceResponse.class);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.oil()).isEmpty();
        assertThat(response.getErrorMessage()).isEqualTo("missing RESULT.OIL");
    }

    @Test
    @DisplayName("RESULT 자체가 없으면 빈 oil 목록과 실패 메시지를 반환한다")
    void missingResult_returnsEmptyOilAndErrorMessage() throws Exception {
        OpinetAverageOilPriceResponse response =
                new ObjectMapper().readValue("{}", OpinetAverageOilPriceResponse.class);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.oil()).isEmpty();
        assertThat(response.getErrorMessage()).isEqualTo("missing RESULT.OIL");
    }
}
