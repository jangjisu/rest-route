package com.restroute.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;

/**
 * 가짜 오피넷(국가 평균 유가) 응답을 정의한다. 경로와 필드명은 실제 {@code OpinetFeignClient}와
 * {@code OpinetAverageOilPriceResponse}에 선언된 값과 같아야 한다.
 */
public final class OpinetApiStubs {

    private static final String AVERAGE_ALL_PRICE_PATH = "/api/avgAllPrice.do";

    private OpinetApiStubs() {}

    /** 휘발유·경유·LPG 평균가 3건을 오늘 날짜로 돌려준다. */
    public static void stubAverageOilPrices(
            WireMockServer opinet, String tradeDate, String gasolinePrice, String dieselPrice, String lpgPrice) {
        opinet.stubFor(get(urlPathEqualTo(AVERAGE_ALL_PRICE_PATH)).willReturn(okJson("""
                {
                  "RESULT": {
                    "OIL": [
                      {"TRADE_DT": "%s", "PRODCD": "B027", "PRODNM": "휘발유", "PRICE": "%s", "DIFF": "-1.23"},
                      {"TRADE_DT": "%s", "PRODCD": "D047", "PRODNM": "경유", "PRICE": "%s", "DIFF": "0.45"},
                      {"TRADE_DT": "%s", "PRODCD": "K015", "PRODNM": "LPG", "PRICE": "%s", "DIFF": "0.00"}
                    ]
                  }
                }
                """.formatted(
                        tradeDate, gasolinePrice, tradeDate, dieselPrice, tradeDate, lpgPrice))));
    }

    /** RESULT.OIL이 비어 있다 — {@code isSuccess()}가 false가 되는 갈래. */
    public static void stubAverageOilPricesMissingResult(WireMockServer opinet) {
        opinet.stubFor(get(urlPathEqualTo(AVERAGE_ALL_PRICE_PATH)).willReturn(okJson("{ \"RESULT\": null }")));
    }

    /** 서버가 상태코드로 실패를 알린다. */
    public static void stubAverageOilPricesStatus(WireMockServer opinet, int status) {
        opinet.stubFor(get(urlPathEqualTo(AVERAGE_ALL_PRICE_PATH))
                .willReturn(aResponse().withStatus(status).withBody("{\"errorType\":\"stub\"}")));
    }

    /** 연결이 끊긴다 — 상태코드조차 받지 못하는 갈래. */
    public static void stubAverageOilPricesConnectionReset(WireMockServer opinet) {
        opinet.stubFor(get(urlPathEqualTo(AVERAGE_ALL_PRICE_PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }
}
