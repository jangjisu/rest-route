package com.restroute.reststop.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.common.client.response.ExApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RestStopDetailResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("code가 SUCCESS이면 성공으로 판단한다")
    void isSuccess_returnsTrueForSuccessCode() {
        RestStopDetailResponse response = new RestStopDetailResponse();
        ReflectionTestUtils.setField(response, "code", "SUCCESS");

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("code가 SUCCESS가 아니면 실패로 판단한다")
    void isSuccess_returnsFalseForOtherCode() {
        RestStopDetailResponse response = new RestStopDetailResponse();
        ReflectionTestUtils.setField(response, "code", "ERROR");

        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("pageSize를 총 페이지 수로 변환한다")
    void getTotalPageCount_returnsParsedValue() {
        RestStopDetailResponse response = new RestStopDetailResponse();
        ReflectionTestUtils.setField(response, "pageSize", "3");

        assertThat(response.getTotalPageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("pageSize가 숫자가 아니면 총 페이지 수 기본값 1을 반환한다")
    void getTotalPageCount_returnsDefaultWhenInvalid() {
        RestStopDetailResponse response = new RestStopDetailResponse();
        ReflectionTestUtils.setField(response, "pageSize", "abc");

        assertThat(response.getTotalPageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("편의시설 API 서버 오류 응답의 메시지를 반환한다")
    void readValue_returnsUpstreamExceptionMessage() throws Exception {
        String json = """
                {
                  "exception": {
                    "cause": null,
                    "message": "For input string: \\"\\"",
                    "localizedMessage": "For input string: \\"\\"",
                    "stackTrace": []
                  }
                }
                """;

        RestStopDetailResponse response = objectMapper.readValue(json, RestStopDetailResponse.class);

        assertThat(response).isInstanceOf(ExApiResponse.class);
        assertThat(response.getErrorMessage()).isEqualTo("For input string: \"\"");
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("편의시설 API 일반 실패 응답의 메시지를 반환한다")
    void getErrorMessage_returnsResponseMessageWithoutException() {
        RestStopDetailResponse response = new RestStopDetailResponse();
        ReflectionTestUtils.setField(response, "message", "인증키가 유효하지 않습니다.");

        assertThat(response.getErrorMessage()).isEqualTo("인증키가 유효하지 않습니다.");
    }
}
