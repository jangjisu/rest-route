package com.restroute.service.image.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopNotFoundExceptionTest {

    @Test
    @DisplayName("휴게소 코드로 404 예외를 생성한다")
    void forServiceAreaCode_createsExceptionWithCode() {
        RestStopNotFoundException exception = RestStopNotFoundException.forServiceAreaCode("A00001");

        assertThat(exception.getMessage()).isEqualTo("Rest stop not found: A00001");
    }
}
