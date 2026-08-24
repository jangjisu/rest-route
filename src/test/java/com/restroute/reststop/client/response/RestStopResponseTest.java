package com.restroute.reststop.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.common.client.response.ExApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RestStopResponseTest {

    @Test
    @DisplayName("code가 SUCCESS이면 성공으로 판단한다")
    void isSuccess_returnsTrueForSuccessCode() {
        RestStopResponse response = new RestStopResponse();
        ReflectionTestUtils.setField(response, "code", "SUCCESS");

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("code가 SUCCESS가 아니면 실패로 판단한다")
    void isSuccess_returnsFalseForOtherCode() {
        RestStopResponse response = new RestStopResponse();
        ReflectionTestUtils.setField(response, "code", "ERROR");

        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("EX API 공통 응답으로 일반 실패 메시지를 반환한다")
    void getErrorMessage_returnsResponseMessage() {
        RestStopResponse response = new RestStopResponse();
        ReflectionTestUtils.setField(response, "message", "인증키가 유효하지 않습니다.");

        assertThat(response).isInstanceOf(ExApiResponse.class);
        assertThat(response.getErrorMessage()).isEqualTo("인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("pageSize를 총 페이지 수로 변환한다")
    void getTotalPageCount_returnsParsedValue() {
        RestStopResponse response = new RestStopResponse();
        ReflectionTestUtils.setField(response, "pageSize", "20");

        assertThat(response.getTotalPageCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("pageSize가 숫자가 아니면 총 페이지 수 기본값 1을 반환한다")
    void getTotalPageCount_returnsDefaultWhenInvalid() {
        RestStopResponse response = new RestStopResponse();
        ReflectionTestUtils.setField(response, "pageSize", "abc");

        assertThat(response.getTotalPageCount()).isEqualTo(1);
    }
}
