package com.restroute.oilprice.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestOilPriceResponseTest {

    @Test
    @DisplayName("SUCCESS 코드를 성공으로 판단한다")
    void isSuccess_returnsTrueForSuccessCode() throws Exception {
        RestOilPriceResponse response =
                new ObjectMapper().readValue("{\"code\":\"SUCCESS\"}", RestOilPriceResponse.class);

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("SUCCESS 외 코드는 실패로 판단한다")
    void isSuccess_returnsFalseForNonSuccessCode() throws Exception {
        RestOilPriceResponse response =
                new ObjectMapper().readValue("{\"code\":\"ERROR\"}", RestOilPriceResponse.class);

        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("오류 메시지는 최상위 message 값을 반환한다")
    void getErrorMessage_returnsMessage() throws Exception {
        RestOilPriceResponse response =
                new ObjectMapper().readValue("{\"message\":\"인증키가 유효하지 않습니다.\"}", RestOilPriceResponse.class);

        assertThat(response.getErrorMessage()).isEqualTo("인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("curStateStation 응답의 페이지와 list를 매핑한다")
    void readValue_mapsPaginationAndList() throws Exception {
        String json = """
                {
                  "count": 226,
                  "pageNo": 1,
                  "numOfRows": 99,
                  "pageSize": 3,
                  "message": "인증키가 유효합니다.",
                  "code": "SUCCESS",
                  "list": [
                    {
                      "routeCode": "0010",
                      "serviceAreaCode": "B00001",
                      "serviceAreaName": "서울만남(부산)주유소",
                      "serviceAreaCode2": "000002"
                    }
                  ]
                }
                """;

        RestOilPriceResponse response = new ObjectMapper().readValue(json, RestOilPriceResponse.class);

        assertThat(response.getCode()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("인증키가 유효합니다.");
        assertThat(response.getCount()).isEqualTo(226);
        assertThat(response.getPageNo()).isEqualTo(1);
        assertThat(response.getNumOfRows()).isEqualTo(99);
        assertThat(response.getPageSize()).isEqualTo(3);
        assertThat(response.getList()).hasSize(1);
        assertThat(response.getList().get(0).getServiceAreaCode2()).isEqualTo("000002");
    }
}
