package com.restroute.reststopcontent.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestBestfoodResponseTest {

    @Test
    @DisplayName("SUCCESS 코드를 성공으로 판단한다")
    void isSuccess_returnsTrueForSuccessCode() throws Exception {
        RestBestfoodResponse response =
                new ObjectMapper().readValue("{\"code\":\"SUCCESS\"}", RestBestfoodResponse.class);

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("SUCCESS 외 코드는 실패로 판단한다")
    void isSuccess_returnsFalseForNonSuccessCode() throws Exception {
        RestBestfoodResponse response =
                new ObjectMapper().readValue("{\"code\":\"ERROR\"}", RestBestfoodResponse.class);

        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("오류 메시지는 최상위 message 값을 반환한다")
    void getErrorMessage_returnsMessage() throws Exception {
        RestBestfoodResponse response =
                new ObjectMapper().readValue("{\"message\":\"인증키가 유효하지 않습니다.\"}", RestBestfoodResponse.class);

        assertThat(response.getErrorMessage()).isEqualTo("인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("restBestfoodList 응답의 페이지와 list를 매핑한다")
    void readValue_mapsPaginationAndList() throws Exception {
        String json = """
                {
                  "count": 7214,
                  "pageNo": 1,
                  "numOfRows": 99,
                  "pageSize": 73,
                  "message": "인증키가 유효합니다.",
                  "code": "SUCCESS",
                  "list": [
                    {
                      "stdRestCd": "000001",
                      "stdRestNm": "서울만남(부산)휴게소",
                      "foodNm": "농심어묵우동",
                      "recommendyn": "Y"
                    }
                  ]
                }
                """;

        RestBestfoodResponse response = new ObjectMapper().readValue(json, RestBestfoodResponse.class);

        assertThat(response.getCode()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("인증키가 유효합니다.");
        assertThat(response.getCount()).isEqualTo(7214);
        assertThat(response.getPageNo()).isEqualTo(1);
        assertThat(response.getNumOfRows()).isEqualTo(99);
        assertThat(response.getPageSize()).isEqualTo(73);
        assertThat(response.getList()).hasSize(1);
        assertThat(response.getList().get(0).getStdRestCd()).isEqualTo("000001");
        assertThat(response.getList().get(0).getRecommendyn()).isEqualTo("Y");
    }
}
