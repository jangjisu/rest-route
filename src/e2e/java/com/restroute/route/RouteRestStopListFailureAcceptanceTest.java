package com.restroute.route;

import static com.restroute.support.KakaoApiStubs.stubDirectionsResultCode;
import static com.restroute.support.KakaoApiStubs.stubDirectionsStatus;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearch;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchDelay;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchEmpty;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchNonJson;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchStatus;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 카카오가 실패할 때 사용자에게 실제로 무엇이 나가는지를 고정한다. 여기 적힌 기대값은
 * 설계 의도가 아니라 <b>2026-09-09 시점에 관찰된 현재 동작</b>이다 — 회복탄력성 작업으로
 * 동작을 바꾸면 이 테스트들이 빨간불이 되면서 "무엇이 어떻게 달라졌는지"를 드러낸다.
 *
 * <p>관찰된 것 중 눈에 띄는 두 가지:
 *
 * <ul>
 *   <li>외부 API 실패는 모두 HTTP <b>200</b>으로 나간다 — {@code EXTERNAL_API_UNAVAILABLE}이
 *       {@code HttpStatus.OK}로 정의돼 있어서다. 실패를 본문 code로만 알린다.
 *   <li>500·429·401·비JSON 응답이 <b>전부 같은 결과</b>가 된다. 쿼터 초과와 서버 장애와 키
 *       만료는 대응이 달라야 하는데 지금은 구분되지 않는다.
 * </ul>
 *
 * <p>비즈니스적 실패(검색 결과 없음, 경로 없음)는 이와 별개로 404 + 구체적 안내 문구가
 * 나간다 — 그쪽은 의도대로 동작한다.
 */
class RouteRestStopListFailureAcceptanceTest extends AcceptanceTest {

    private static final double ORIGIN_LONGITUDE = 126.9780;
    private static final double ORIGIN_LATITUDE = 37.5665;

    private static final String EXTERNAL_API_UNAVAILABLE = "EXTERNAL_API_UNAVAILABLE";
    private static final int SUCCESS_STATUS = 200;
    private static final int NOT_FOUND_STATUS = 404;

    /** 기본 readTimeout. 이 값을 넘겨야 끊기는지 보려고 지연을 이보다 길게 준다. */
    private static final int READ_TIMEOUT_MILLIS = 10_000;

    private static final int DELAY_OVER_TIMEOUT_MILLIS = 15_000;

    @Test
    @DisplayName("지오코딩이 HTTP 500이면 200 + EXTERNAL_API_UNAVAILABLE로 나간다")
    void geocodingServerError_respondsExternalApiUnavailable() {
        stubKeywordSearchStatus(KAKAO, 500);

        requestRouteRestStops()
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE))
                .body("data", nullValue());
    }

    @Test
    @DisplayName("지오코딩이 HTTP 429여도 서버 장애와 똑같이 처리된다")
    void geocodingQuotaExceeded_isNotDistinguishedFromServerError() {
        stubKeywordSearchStatus(KAKAO, 429);

        requestRouteRestStops().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    @Test
    @DisplayName("에러 응답 본문이 JSON이 아니어도 파싱 실패로 새지 않는다")
    void geocodingNonJsonErrorBody_respondsExternalApiUnavailable() {
        stubKeywordSearchNonJson(KAKAO, 503);

        requestRouteRestStops().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    @Test
    @DisplayName("길찾기가 HTTP 500이면 체인 뒷단 실패도 같은 응답이 된다")
    void directionsServerError_respondsExternalApiUnavailable() {
        stubKeywordSearch(KAKAO, "부산역", 129.0403, 35.1148);
        stubDirectionsStatus(KAKAO, 500);

        requestRouteRestStops().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    /**
     * 이 테스트만 느리다(10초). 그래도 남기는 건, 설정한 readTimeout이 실제로 걸리는지는
     * Mockito로 클라이언트를 바꿔치기하는 단위 테스트로는 확인할 방법이 없어서다.
     *
     * <p>동시에 두 가지를 기록한다 — 재시도가 없다는 것(있었다면 20초·30초가 됐을 것),
     * 그리고 사용자가 <b>실패를 알기까지 10초를 기다린다</b>는 것.
     */
    @Test
    @DisplayName("지오코딩이 늦으면 readTimeout에서 끊기고 재시도 없이 한 번만 시도한다")
    void geocodingSlowerThanReadTimeout_failsOnceAfterTimeout() {
        stubKeywordSearchDelay(KAKAO, DELAY_OVER_TIMEOUT_MILLIS);

        Instant startedAt = Instant.now();
        requestRouteRestStops().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();

        assertThat(elapsedMillis)
                .as("readTimeout에서 끊겼다면 10초를 갓 넘겨야 하고, 재시도가 붙었다면 그 배수가 된다")
                .isBetween((long) READ_TIMEOUT_MILLIS, (long) DELAY_OVER_TIMEOUT_MILLIS);
    }

    @Test
    @DisplayName("목적지를 못 찾으면 404와 함께 무엇을 못 찾았는지 알려준다")
    void destinationNotFound_responds404WithReason() {
        stubKeywordSearchEmpty(KAKAO);

        requestRouteRestStops()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("목적지 검색 결과가 없습니다: 부산역"));
    }

    @Test
    @DisplayName("길찾기가 경로를 못 찾으면 404와 함께 결과 코드별 안내 문구가 나간다")
    void routeNotFound_responds404WithGuidance() {
        stubKeywordSearch(KAKAO, "부산역", 129.0403, 35.1148);
        stubDirectionsResultCode(KAKAO, 104);

        requestRouteRestStops()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("출발지와 도착지가 너무 가까워요. 좀 더 떨어진 위치를 선택해주세요."));
    }

    /**
     * 실패 경로만 보므로 휴게소를 심지 않는다 — 어느 케이스든 휴게소 조회 앞단에서 끝난다.
     */
    private Response requestRouteRestStops() {
        RequestSpecification request = given().param("originLat", ORIGIN_LATITUDE)
                .param("originLng", ORIGIN_LONGITUDE)
                .param("destinationQuery", "부산역");
        return request.when().get("/api/route-rest-stops/list");
    }
}
