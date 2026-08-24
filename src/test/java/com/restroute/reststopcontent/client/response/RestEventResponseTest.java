package com.restroute.reststopcontent.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.common.client.response.ExApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RestEventResponseTest {

    @Test
    @DisplayName("code가 SUCCESS이면 성공으로 판단한다")
    void isSuccess_returnsTrueForSuccessCode() {
        RestEventResponse response = new RestEventResponse();
        ReflectionTestUtils.setField(response, "code", "SUCCESS");

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("code가 SUCCESS가 아니면 실패로 판단한다")
    void isSuccess_returnsFalseForOtherCode() {
        RestEventResponse response = new RestEventResponse();
        ReflectionTestUtils.setField(response, "code", "ERROR");

        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("EX API 공통 응답으로 일반 실패 메시지를 반환한다")
    void getErrorMessage_returnsResponseMessage() {
        RestEventResponse response = new RestEventResponse();
        ReflectionTestUtils.setField(response, "message", "인증키가 유효하지 않습니다.");

        assertThat(response).isInstanceOf(ExApiResponse.class);
        assertThat(response.getErrorMessage()).isEqualTo("인증키가 유효하지 않습니다.");
    }
}
