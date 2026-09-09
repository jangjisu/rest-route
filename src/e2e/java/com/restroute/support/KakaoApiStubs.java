package com.restroute.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.StringJoiner;

/**
 * 가짜 카카오 서버의 응답을 정의한다. 경로와 파라미터는 실제 Feign 인터페이스
 * ({@code KakaoLocalFeignClient}, {@code KakaoNaviFeignClient})에 선언된 값과 같아야 하며,
 * 다르면 WireMock이 404를 돌려주므로 스텁이 어긋난 걸 바로 알 수 있다.
 */
public final class KakaoApiStubs {

    private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
    private static final String DIRECTIONS_PATH = "/v1/directions";

    /** 길찾기가 성공했음을 뜻하는 값. 0이 아니면 서비스가 NotFound로 끝낸다. */
    private static final int RESULT_CODE_SUCCESS = 0;

    private KakaoApiStubs() {}

    /** 지오코딩 — 목적지 검색어 하나를 좌표로 바꿔 돌려준다. */
    public static void stubKeywordSearch(WireMockServer kakao, String placeName, double longitude, double latitude) {
        kakao.stubFor(get(urlPathEqualTo(KEYWORD_SEARCH_PATH))
                .willReturn(okJson("""
                        {
                          "documents": [
                            {
                              "x": "%s",
                              "y": "%s",
                              "place_name": "%s",
                              "address_name": "%s 주소"
                            }
                          ]
                        }
                        """.formatted(longitude, latitude, placeName, placeName))));
    }

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

    private static String join(double... vertexes) {
        StringJoiner joiner = new StringJoiner(",");
        for (double vertex : vertexes) {
            joiner.add(String.valueOf(vertex));
        }
        return joiner.toString();
    }
}
