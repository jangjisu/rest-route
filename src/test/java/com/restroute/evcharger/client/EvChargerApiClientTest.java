package com.restroute.evcharger.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.common.client.exception.ExApiException;
import com.restroute.evcharger.client.response.EvChargerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class EvChargerApiClientTest {

    @Mock
    private EvChargerFeignClient evChargerFeignClient;

    private EvChargerApiClient evChargerApiClient;

    @BeforeEach
    void setUp() {
        evChargerApiClient =
                new EvChargerApiClient(evChargerFeignClient, "https://apis.data.go.kr/B552584/EvCharger", "test-key");
    }

    @Test
    @DisplayName("전기차 충전소 API에 JSON, 200건, C0 필터와 인증키를 전달한다")
    void getChargerInfo_appliesRequestParameters() throws Exception {
        EvChargerResponse response = new ObjectMapper()
                .readValue("{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL SERVICE.\"}", EvChargerResponse.class);
        when(evChargerFeignClient.getChargerInfo("test-key", 2, 200, "JSON", "C0"))
                .thenReturn(response);

        assertThat(evChargerApiClient.getChargerInfo(2)).isSameAs(response);

        verify(evChargerFeignClient).getChargerInfo("test-key", 2, 200, "JSON", "C0");
    }

    @Test
    @DisplayName("전기차 API 오류 로그용 URL에는 serviceKey를 노출하지 않는다")
    void getChargerInfo_redactsServiceKeyWhenApiFails() throws Exception {
        EvChargerResponse response = new ObjectMapper()
                .readValue("{\"resultCode\":\"99\",\"resultMsg\":\"failed\"}", EvChargerResponse.class);
        when(evChargerFeignClient.getChargerInfo("test-key", 1, 200, "JSON", "C0"))
                .thenReturn(response);

        assertThatThrownBy(() -> evChargerApiClient.getChargerInfo(1))
                .isInstanceOf(ExApiException.class)
                .hasMessageContaining("serviceKey=<redacted>")
                .hasMessageNotContaining("test-key");
    }

    @Test
    @DisplayName("Feign 예외 로그에도 serviceKey를 노출하지 않는다")
    void getChargerInfo_doesNotExposeServiceKeyInExceptionLog(CapturedOutput output) {
        when(evChargerFeignClient.getChargerInfo("test-key", 1, 200, "JSON", "C0"))
                .thenThrow(new RuntimeException("[504 Gateway Time-out] serviceKey=secret-key&pageNo=1"));

        assertThatThrownBy(() -> evChargerApiClient.getChargerInfo(1)).isInstanceOf(ExApiException.class);

        assertThat(output).doesNotContain("secret-key").contains("serviceKey=<redacted>");
    }
}
