package com.restroute.oilprice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.exception.ExApiException;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpinetApiClientTest {

    @Mock
    private OpinetFeignClient opinetFeignClient;

    private OpinetApiClient opinetApiClient;

    @BeforeEach
    void setUp() {
        opinetApiClient = new OpinetApiClient(
                opinetFeignClient, new ObjectMapper(), "https://www.opinet.co.kr", "test-opinet-key");
    }

    @Test
    @DisplayName("전국 평균가 API 호출 시 String JSON 응답을 파싱한다")
    void getAverageOilPrices_appliesDefaultParameters() {
        when(opinetFeignClient.getAverageOilPrices("json", "test-opinet-key")).thenReturn("""
                        {
                          "RESULT": {
                            "OIL": [
                              {
                                "TRADE_DT": "20260707",
                                "PRODCD": "B027",
                                "PRODNM": "휘발유",
                                "PRICE": "1892.88",
                                "DIFF": "-4.19"
                              }
                            ]
                          }
                        }
                        """);

        OpinetAverageOilPriceResponse result = opinetApiClient.getAverageOilPrices();

        assertThat(result.oil()).hasSize(1);
        assertThat(result.oil().get(0).getProductCode()).isEqualTo("B027");
    }

    @Test
    @DisplayName("전국 평균가 API 실패에 마스킹된 요청 URL과 오류 메시지를 포함한다")
    void getAverageOilPrices_includesRequestUrlWhenApiFails() {
        when(opinetFeignClient.getAverageOilPrices("json", "test-opinet-key")).thenReturn("{}");

        assertThatThrownBy(opinetApiClient::getAverageOilPrices)
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://www.opinet.co.kr/api/avgAllPrice.do?out=json&code=<redacted>, message=missing RESULT.OIL");
    }

    @Test
    @DisplayName("전국 평균가 API 빈 응답에 마스킹된 요청 URL을 포함한다")
    void getAverageOilPrices_includesRequestUrlWhenResponseIsEmpty() {
        when(opinetFeignClient.getAverageOilPrices("json", "test-opinet-key")).thenReturn(null);

        assertThatThrownBy(opinetApiClient::getAverageOilPrices)
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://www.opinet.co.kr/api/avgAllPrice.do?out=json&code=<redacted>, message=empty response");
    }

    @Test
    @DisplayName("전국 평균가 API 공백 응답도 빈 응답으로 처리한다")
    void getAverageOilPrices_treatsBlankResponseAsEmpty() {
        when(opinetFeignClient.getAverageOilPrices("json", "test-opinet-key")).thenReturn(" ");

        assertThatThrownBy(opinetApiClient::getAverageOilPrices)
                .isInstanceOf(ExApiException.class)
                .hasMessageContaining("message=empty response")
                .hasMessageNotContaining("test-opinet-key");
    }

    @Test
    @DisplayName("전국 평균가 Feign 예외에 마스킹된 요청 URL과 원인 메시지를 포함한다")
    void getAverageOilPrices_includesRequestUrlWhenFeignCallFails() {
        when(opinetFeignClient.getAverageOilPrices("json", "test-opinet-key"))
                .thenThrow(new IllegalStateException("network down"));

        assertThatThrownBy(opinetApiClient::getAverageOilPrices)
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://www.opinet.co.kr/api/avgAllPrice.do?out=json&code=<redacted>, message=network down")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("전국 평균가 API JSON 파싱 실패에도 인증키를 노출하지 않는다")
    void getAverageOilPrices_hidesApiKeyWhenJsonParsingFails() {
        when(opinetFeignClient.getAverageOilPrices("json", "test-opinet-key")).thenReturn("<html></html>");

        assertThatThrownBy(opinetApiClient::getAverageOilPrices)
                .isInstanceOf(ExApiException.class)
                .hasMessageContaining("code=<redacted>")
                .hasMessageNotContaining("test-opinet-key")
                .hasMessageContaining("invalid JSON response");
    }
}
