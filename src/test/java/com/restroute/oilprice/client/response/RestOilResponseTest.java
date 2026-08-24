package com.restroute.oilprice.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.ExApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RestOilResponseTest {

    @Test
    @DisplayName("code가 SUCCESS이면 성공으로 판단한다")
    void isSuccess_returnsTrueForSuccessCode() {
        RestOilResponse response = new RestOilResponse();
        ReflectionTestUtils.setField(response, "code", "SUCCESS");

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("code가 SUCCESS가 아니면 실패로 판단한다")
    void isSuccess_returnsFalseForOtherCode() {
        RestOilResponse response = new RestOilResponse();
        ReflectionTestUtils.setField(response, "code", "ERROR");

        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("EX API 공통 응답으로 일반 실패 메시지를 반환한다")
    void getErrorMessage_returnsResponseMessage() {
        RestOilResponse response = new RestOilResponse();
        ReflectionTestUtils.setField(response, "message", "인증키가 유효하지 않습니다.");

        assertThat(response).isInstanceOf(ExApiResponse.class);
        assertThat(response.getErrorMessage()).isEqualTo("인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("실측 응답의 숫자 count를 역직렬화한다")
    void readValue_mapsNumericCount() throws Exception {
        String json = """
                {
                  "count": 429,
                  "list": [],
                  "message": "인증키가 유효합니다.",
                  "code": "SUCCESS"
                }
                """;

        RestOilResponse response = new ObjectMapper().readValue(json, RestOilResponse.class);

        assertThat(response.getCount()).isEqualTo(429);
        assertThat(response.getList()).isEmpty();
    }
}
