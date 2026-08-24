package com.restroute.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExternalApiRequestLogTest {

    @Test
    @DisplayName("외부 API URL의 인증키 파라미터를 마스킹한다")
    void sanitizeUrl_masksSensitiveParameters() {
        String sanitized = ExternalApiRequestLog.sanitizeUrl(
                "https://example.com/api?key=ex-key&serviceKey=ev-key&type=json&code=opinet-key&pageNo=1");

        assertThat(sanitized)
                .isEqualTo(
                        "https://example.com/api?key=<redacted>&serviceKey=<redacted>&type=json&code=<redacted>&pageNo=1")
                .doesNotContain("ex-key")
                .doesNotContain("ev-key")
                .doesNotContain("opinet-key");
    }

    @Test
    @DisplayName("빈 URL은 그대로 반환한다")
    void sanitizeUrl_keepsEmptyValues() {
        assertThat(ExternalApiRequestLog.sanitizeUrl(null)).isNull();
        assertThat(ExternalApiRequestLog.sanitizeUrl(" ")).isEqualTo(" ");
    }
}
