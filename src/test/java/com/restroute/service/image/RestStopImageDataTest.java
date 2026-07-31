package com.restroute.service.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopImageDataTest {

    @Test
    @DisplayName("byte 배열 내용이 같으면 동등하다")
    void equals_comparesArrayContent() {
        RestStopImageData first = new RestStopImageData(new byte[] {1, 2, 3}, new byte[] {4, 5});
        RestStopImageData second = new RestStopImageData(new byte[] {1, 2, 3}, new byte[] {4, 5});

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    @DisplayName("byte 배열 내용이 다르면 동등하지 않다")
    void equals_returnsFalseWhenArrayContentDiffers() {
        RestStopImageData first = new RestStopImageData(new byte[] {1, 2, 3}, new byte[] {4, 5});
        RestStopImageData second = new RestStopImageData(new byte[] {1, 2, 9}, new byte[] {4, 5});

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("자기 자신과는 항상 동등하다")
    void equals_returnsTrueForSameReference() {
        RestStopImageData data = new RestStopImageData(new byte[] {1}, new byte[] {2});

        assertThat(data).isEqualTo(data);
    }

    @Test
    @DisplayName("다른 타입이나 null과는 동등하지 않다")
    void equals_returnsFalseForDifferentTypeOrNull() {
        RestStopImageData data = new RestStopImageData(new byte[] {1}, new byte[] {2});

        assertThat(data).isNotEqualTo("문자열");
        assertThat(data).isNotEqualTo(null);
    }

    @Test
    @DisplayName("두 번째 배열 내용만 다르면 동등하지 않다")
    void equals_returnsFalseWhenSecondArrayContentDiffers() {
        RestStopImageData first = new RestStopImageData(new byte[] {1, 2, 3}, new byte[] {4, 5});
        RestStopImageData second = new RestStopImageData(new byte[] {1, 2, 3}, new byte[] {4, 9});

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("toString은 배열 내용을 사람이 읽을 수 있는 형태로 보여준다")
    void toString_includesArrayContent() {
        RestStopImageData data = new RestStopImageData(new byte[] {1, 2}, new byte[] {3});

        assertThat(data.toString()).contains("[1, 2]").contains("[3]");
    }
}
