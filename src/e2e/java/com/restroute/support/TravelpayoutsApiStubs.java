package com.restroute.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;

/**
 * 가짜 Travelpayouts(항공권 실 검색) 응답을 정의한다. 경로와 필드명은 실제
 * {@code TravelpayoutsFeignClient}와 {@code TravelpayoutsGroupedPricesResponse}에 선언된
 * 값과 같아야 한다. RANGE·FIXED 둘 다 같은 경로({@code grouped_prices})를 쓰므로 구분 없이 하나로 둔다.
 */
public final class TravelpayoutsApiStubs {

    private static final String GROUPED_PRICES_PATH = "/aviasales/v3/grouped_prices";

    private TravelpayoutsApiStubs() {}

    /**
     * 딜 하나가 담긴 성공 응답. destination이 map의 키이자 응답 항목의 destination 필드다.
     *
     * <p>{@code departureAt}/{@code returnAt}은 {@code OffsetDateTime.parse()}로 파싱되므로
     * ({@code FlightDealResponseMapper.mapOne()}) 오프셋까지 포함한 ISO-8601이어야 한다(예:
     * {@code "2026-10-18T09:00:00+09:00"}) — 오프셋이 없으면 파싱은 실패하지 않지만 애초에
     * 둘 중 하나라도 비어 있으면 항목 자체가 조용히 걸러진다.
     */
    public static void stubGroupedPrices(
            WireMockServer travelpayouts,
            String destination,
            int price,
            String departureAt,
            String returnAt,
            String flightNumber) {
        travelpayouts.stubFor(get(urlPathEqualTo(GROUPED_PRICES_PATH)).willReturn(okJson("""
                {
                  "success": true,
                  "currency": "krw",
                  "data": {
                    "%s": {
                      "origin": "ICN",
                      "destination": "%s",
                      "origin_airport": "ICN",
                      "destination_airport": "%s",
                      "price": %d,
                      "airline": "KE",
                      "flight_number": "%s",
                      "departure_at": "%s",
                      "return_at": "%s",
                      "transfers": 0,
                      "return_transfers": 0,
                      "duration": 150,
                      "duration_to": 150,
                      "duration_back": 0,
                      "gate": "stub-gate",
                      "link": "/stub-link"
                    }
                  }
                }
                """.formatted(
                        destination, destination, destination, price, flightNumber, departureAt, returnAt))));
    }

    /** 검색 결과가 없다 — success는 true인데 data가 빈 갈래. */
    public static void stubGroupedPricesEmpty(WireMockServer travelpayouts) {
        travelpayouts.stubFor(get(urlPathEqualTo(GROUPED_PRICES_PATH))
                .willReturn(okJson("{ \"success\": true, \"currency\": \"krw\", \"data\": {} }")));
    }

    /** success 필드 자체가 false다 — HTTP는 200이지만 API 차원의 실패. */
    public static void stubGroupedPricesUnsuccessful(WireMockServer travelpayouts) {
        travelpayouts.stubFor(get(urlPathEqualTo(GROUPED_PRICES_PATH))
                .willReturn(okJson("{ \"success\": false, \"currency\": \"krw\", \"data\": {} }")));
    }

    /** 서버가 상태코드로 실패를 알린다. */
    public static void stubGroupedPricesStatus(WireMockServer travelpayouts, int status) {
        travelpayouts.stubFor(get(urlPathEqualTo(GROUPED_PRICES_PATH))
                .willReturn(aResponse().withStatus(status).withBody("{\"errorType\":\"stub\"}")));
    }

    /** 연결이 끊긴다 — 상태코드조차 받지 못하는 갈래. */
    public static void stubGroupedPricesConnectionReset(WireMockServer travelpayouts) {
        travelpayouts.stubFor(get(urlPathEqualTo(GROUPED_PRICES_PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    /** grouped_prices가 호출되지 않았음을 확인한다 — 검증 실패로 외부에 나가기 전에 끝났다는 뜻. */
    public static void verifyGroupedPricesNotCalled(WireMockServer travelpayouts) {
        travelpayouts.verify(0, getRequestedFor(urlPathEqualTo(GROUPED_PRICES_PATH)));
    }
}
