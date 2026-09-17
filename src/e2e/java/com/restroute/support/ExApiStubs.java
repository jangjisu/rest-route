package com.restroute.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;

/**
 * 가짜 고속도로 공공 API(ExApi)의 응답을 정의한다. 경로와 필드명은 실제 Feign 인터페이스
 * ({@code ExApiFeignClient})와 응답 DTO에 선언된 값과 같아야 하며, 다르면 WireMock이 404를
 * 돌려주거나 역직렬화가 비어서 스텁이 어긋난 걸 바로 알 수 있다.
 *
 * <p>지금은 유가 갱신이 쓰는 주유소 현황 하나만 둔다 — 나머지 ExApi 경로는 배치 동기화가 쓰고
 * 요청 시점에 불리지 않아 인수 테스트 대상이 아니다.
 */
public final class ExApiStubs {

    private static final String CUR_STATE_STATION_PATH = "/openapi/business/curStateStation";

    private ExApiStubs() {}

    /**
     * 주유소 현황 — 유가 한 건을 돌려준다. 경유 필드만 JSON 이름이 {@code diselPrice}로 다르며
     * (공공 API 원본의 철자다) 그 매핑이 실제로 도는지가 이 계층에서 확인할 값 중 하나다.
     */
    public static void stubOilPrice(
            WireMockServer exApi, String serviceAreaCode2, String gasolinePrice, String dieselPrice, String lpgPrice) {
        exApi.stubFor(get(urlPathEqualTo(CUR_STATE_STATION_PATH))
                .willReturn(okJson("""
                {
                  "code": "SUCCESS", "message": "OK", "count": 1,
                  "pageNo": 1, "numOfRows": 1, "pageSize": 1,
                  "list": [
                    {
                      "serviceAreaCode2": "%s",
                      "serviceAreaName": "안성주유소",
                      "oilCompany": "SK에너지",
                      "gasolinePrice": "%s",
                      "diselPrice": "%s",
                      "lpgPrice": "%s",
                      "lpgYn": "Y"
                    }
                  ]
                }
                """.formatted(serviceAreaCode2, gasolinePrice, dieselPrice, lpgPrice))));
    }

    /** 주유소 현황 — 조회는 됐는데 결과가 비어 있다. HTTP 실패와 갈래가 다르다. */
    public static void stubOilPriceEmpty(WireMockServer exApi) {
        exApi.stubFor(get(urlPathEqualTo(CUR_STATE_STATION_PATH))
                .willReturn(okJson("{ \"code\": \"SUCCESS\", \"message\": \"OK\", \"count\": 0, \"list\": [] }")));
    }

    /** 주유소 현황 — 서버가 상태코드로 실패를 알린다. */
    public static void stubOilPriceStatus(WireMockServer exApi, int status) {
        exApi.stubFor(get(urlPathEqualTo(CUR_STATE_STATION_PATH))
                .willReturn(aResponse().withStatus(status).withBody("{\"errorType\":\"stub\"}")));
    }

    /** 주유소 현황 — 연결이 끊긴다. 상태코드조차 받지 못하는 갈래다. */
    public static void stubOilPriceConnectionReset(WireMockServer exApi) {
        exApi.stubFor(get(urlPathEqualTo(CUR_STATE_STATION_PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    /** 주유소 현황이 호출되지 않았음을 확인한다 — 외부에 나가기 전에 끝났다는 뜻. */
    public static void verifyOilPriceNotCalled(WireMockServer exApi) {
        exApi.verify(0, getRequestedFor(urlPathEqualTo(CUR_STATE_STATION_PATH)));
    }
}
