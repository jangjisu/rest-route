package com.restroute.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import java.util.StringJoiner;

/**
 * 가짜 카카오 서버의 응답을 정의한다. 경로와 파라미터는 실제 Feign 인터페이스
 * ({@code KakaoNaviFeignClient})에 선언된 값과 같아야 하며, 다르면 WireMock이 404를 돌려주므로
 * 스텁이 어긋난 걸 바로 알 수 있다.
 *
 * <p>지오코딩(카카오 로컬) 스텁은 없다 — {@code /api/route-rest-stops/list}는 목적지를 좌표로만
 * 받으므로 그쪽을 부르지 않는다. 부르지 않는다는 사실 자체는
 * {@link #verifyKeywordSearchNotCalled(WireMockServer)}로 확인한다.
 */
public final class KakaoApiStubs {

    private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
    private static final String DIRECTIONS_PATH = "/v1/directions";

    /** 길찾기가 성공했음을 뜻하는 값. 0이 아니면 서비스가 NotFound로 끝낸다. */
    private static final int RESULT_CODE_SUCCESS = 0;

    private KakaoApiStubs() {}

    /**
     * 길찾기 — 정점 하나짜리 도로 구간을 담은 경로 1개를 돌려준다.
     *
     * @param vertexes 카카오 계약대로 [경도, 위도, 경도, 위도, ...] 로 평탄화한 좌표열.
     *     {@code RouteCoordinateReducer}는 정점이 300개 이하면 축소하지 않고 그대로 쓰므로
     *     몇 개만 줘도 그 좌표가 그대로 경로가 된다.
     */
    public static void stubDirections(WireMockServer kakao, long distanceMeters, double... vertexes) {
        kakao.stubFor(get(urlPathEqualTo(DIRECTIONS_PATH))
                .willReturn(okJson("""
                        {
                          "routes": [
                            {
                              "result_code": %d,
                              "summary": { "distance": %d, "duration": 600 },
                              "sections": [
                                {
                                  "roads": [
                                    { "name": "테스트도로", "traffic_state": 1, "vertexes": [%s] }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                        """.formatted(RESULT_CODE_SUCCESS, distanceMeters, join(vertexes)))));
    }

    /**
     * 길찾기 — HTTP는 200인데 result_code가 0이 아니다. 카카오가 "경로를 못 찾았다"를
     * 알리는 방식이라 HTTP 실패와는 갈래가 다르다.
     */
    public static void stubDirectionsResultCode(WireMockServer kakao, int resultCode) {
        kakao.stubFor(get(urlPathEqualTo(DIRECTIONS_PATH)).willReturn(okJson("""
                        { "routes": [ { "result_code": %d, "result_msg": "stub" } ] }
                        """.formatted(resultCode))));
    }

    /** 길찾기 — 서버가 상태코드로 실패를 알린다(500, 429, 401 등). */
    public static void stubDirectionsStatus(WireMockServer kakao, int status) {
        kakao.stubFor(get(urlPathEqualTo(DIRECTIONS_PATH))
                .willReturn(aResponse().withStatus(status).withBody("{\"errorType\":\"stub\"}")));
    }

    /**
     * 길찾기 — 상태코드는 실패인데 본문이 JSON이 아니다. 게이트웨이·프록시가 HTML 오류
     * 페이지를 돌려주는 실제 상황을 흉내 낸다.
     */
    public static void stubDirectionsNonJson(WireMockServer kakao, int status) {
        kakao.stubFor(get(urlPathEqualTo(DIRECTIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><body>Service Unavailable</body></html>")));
    }

    /**
     * 길찾기 — 연결이 끊긴다. 서버가 내려갔거나 중간 네트워크가 끊긴 상황으로, 상태코드조차
     * 받지 못하는 갈래라 5xx 응답과는 다르다.
     */
    public static void stubDirectionsConnectionReset(WireMockServer kakao) {
        kakao.stubFor(
                get(urlPathEqualTo(DIRECTIONS_PATH)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    /** 길찾기 — 응답이 늦다. readTimeout(기본 10초)을 넘기면 어떻게 되는지 보기 위한 것. */
    public static void stubDirectionsDelay(WireMockServer kakao, int delayMillis) {
        kakao.stubFor(get(urlPathEqualTo(DIRECTIONS_PATH))
                .willReturn(okJson("{ \"routes\": [] }").withFixedDelay(delayMillis)));
    }

    /** 이 API가 지오코딩을 거치지 않는다는 것을 확인한다. */
    public static void verifyKeywordSearchNotCalled(WireMockServer kakao) {
        kakao.verify(0, getRequestedFor(urlPathEqualTo(KEYWORD_SEARCH_PATH)));
    }

    private static String join(double... vertexes) {
        StringJoiner joiner = new StringJoiner(",");
        for (double vertex : vertexes) {
            joiner.add(String.valueOf(vertex));
        }
        return joiner.toString();
    }
}
