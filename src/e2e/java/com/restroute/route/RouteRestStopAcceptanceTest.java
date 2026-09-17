package com.restroute.route;

import static com.restroute.support.KakaoApiStubs.keywordDocument;
import static com.restroute.support.KakaoApiStubs.stubDirections;
import static com.restroute.support.KakaoApiStubs.stubDirectionsConnectionReset;
import static com.restroute.support.KakaoApiStubs.stubDirectionsResultCode;
import static com.restroute.support.KakaoApiStubs.stubDirectionsStatus;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchConnectionReset;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchDocuments;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchStatus;
import static com.restroute.support.KakaoApiStubs.verifyDirectionsNotCalled;
import static com.restroute.support.KakaoApiStubs.verifyKeywordSearchCalled;
import static com.restroute.support.KakaoApiStubs.verifyKeywordSearchNotCalled;
import static com.restroute.support.RouteFixtures.DESTINATION_LATITUDE;
import static com.restroute.support.RouteFixtures.DESTINATION_LONGITUDE;
import static com.restroute.support.RouteFixtures.KNOWN_DESTINATION_NAME;
import static com.restroute.support.RouteFixtures.ON_ROUTE_NAME;
import static com.restroute.support.RouteFixtures.ORIGIN_LATITUDE;
import static com.restroute.support.RouteFixtures.ORIGIN_LONGITUDE;
import static com.restroute.support.RouteFixtures.ROUTE_DISTANCE_METERS;
import static com.restroute.support.RouteFixtures.ROUTE_VERTEXES;
import static com.restroute.support.RouteFixtures.saveOffRouteRestStop;
import static com.restroute.support.RouteFixtures.saveOnRouteRestStop;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import com.restroute.reststop.repository.RestStopRepository;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 지도 화면이 쓰는 {@code GET /api/route-rest-stops}가 카카오의 응답 형태에 따라 사용자에게
 * 무엇을 돌려주는지를 한자리에 모은다.
 *
 * <p>같은 경로 탐색을 쓰는 {@code /list}와 갈리는 지점은 <b>목적지를 정하는 방식</b>이다.
 * {@code /list}는 서버가 좌표를 아는 이름만 받아 외부 호출이 길찾기 1회인 반면, 지도 화면은
 * <b>검색어를 받아 카카오로 지오코딩</b>하므로 장소 검색까지 최대 2회를 부른다. 그래서
 * {@code /list}가 {@code verifyKeywordSearchNotCalled}로 고정한 것을 여기서는 반대로
 * {@code verifyKeywordSearchCalled}로 고정한다.
 *
 * <p>응답에 경로 요약(거리·시간·통행료·경로선)이 실리는 것도 이 엔드포인트만의 계약이다.
 *
 * <p>반경·상하행·좌표축소 같은 <b>규칙</b>은 단위 테스트가 이미 덮고 있다. 여기서 보는 것은
 * 그 부품들이 실제로 <b>연결되어</b> 돌아가는가, 그리고 <b>외부가 실패할 때 무엇이 나가는가</b>다.
 *
 * <p>실패 케이스의 기대값은 설계 의도가 아니라 <b>관찰된 현재 동작</b>이다 — 관찰 결과 전체는
 * {@code harness/runs/current/kakao-endpoint-failure-as-is.md} 참고.
 */
class RouteRestStopAcceptanceTest extends AcceptanceTest {

    private static final String PATH = "/api/route-rest-stops";

    private static final String EXTERNAL_API_UNAVAILABLE = "EXTERNAL_API_UNAVAILABLE";
    private static final int SUCCESS_STATUS = 200;
    private static final int BAD_REQUEST_STATUS = 400;
    private static final int NOT_FOUND_STATUS = 404;

    private static final String DESTINATION_QUERY = "부산";
    private static final String GEOCODED_NAME = "부산역";
    private static final String GEOCODED_ADDRESS = "부산 동구 초량동";

    @Autowired
    private RestStopRepository restStopRepository;

    private Response get(Map<String, Object> params) {
        return given().params(params).when().get(PATH);
    }

    /** 목적지를 검색어로 지정한다 — 서버가 카카오 장소 검색으로 좌표를 얻어야 한다. */
    private Map<String, Object> destinationQueryParams(String query) {
        return Map.of(
                "originLat", ORIGIN_LATITUDE,
                "originLng", ORIGIN_LONGITUDE,
                "destinationQuery", query);
    }

    /** 목적지를 좌표로 지정한다 — 장소 검색이 필요 없다. */
    private Map<String, Object> destinationCoordinateParams() {
        return Map.of(
                "originLat", ORIGIN_LATITUDE,
                "originLng", ORIGIN_LONGITUDE,
                "destinationLat", DESTINATION_LATITUDE,
                "destinationLng", DESTINATION_LONGITUDE,
                "destinationName", KNOWN_DESTINATION_NAME);
    }

    /** 검색어를 좌표로 바꿔주는 가짜 카카오 응답. 지도 화면의 기본 경로가 이걸 탄다. */
    private void stubGeocoding() {
        stubKeywordSearchDocuments(
                KAKAO,
                keywordDocument(
                        GEOCODED_NAME,
                        GEOCODED_ADDRESS,
                        String.valueOf(DESTINATION_LONGITUDE),
                        String.valueOf(DESTINATION_LATITUDE)));
    }

    // --- 정상 ---------------------------------------------------------------

    /** 검색창에 입력하고 엔터를 친 경우. 좌표를 모르는 채로 이름만 들고 시작한다. */
    @Test
    @DisplayName("목적지 검색어를 주면 장소 검색으로 좌표를 얻어 경로 위 휴게소를 돌려준다")
    void destinationQuery_geocodesThenFindsRestStops() {
        saveOnRouteRestStop(restStopRepository);
        // 경로 밖 휴게소가 빠지는 것이 반경 필터가 실제로 돌았다는 증거다.
        // 하나만 두면 필터가 고장나도 통과해버려 아무것도 증명하지 못한다.
        saveOffRouteRestStop(restStopRepository);

        stubGeocoding();
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data.destination.name", equalTo(GEOCODED_NAME))
                .body("data.destination.latitude", equalTo((float) DESTINATION_LATITUDE))
                .body("data.destination.longitude", equalTo((float) DESTINATION_LONGITUDE))
                .body("data.routes", hasSize(1))
                .body("data.routes[0].restStops", hasSize(1))
                .body("data.routes[0].restStops[0].unitName", equalTo(ON_ROUTE_NAME));

        verifyKeywordSearchCalled(KAKAO, DESTINATION_QUERY);
    }

    /** 검색 후보를 눌러서 고른 경우. 프론트가 이미 좌표를 들고 있어 지오코딩이 필요 없다. */
    @Test
    @DisplayName("목적지 좌표를 직접 주면 장소 검색 없이 경로를 찾는다")
    void destinationCoordinates_skipPlaceSearch() {
        saveOnRouteRestStop(restStopRepository);
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationCoordinateParams())
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.destination.name", equalTo(KNOWN_DESTINATION_NAME))
                .body("data.routes[0].restStops[0].unitName", equalTo(ON_ROUTE_NAME));

        verifyKeywordSearchNotCalled(KAKAO);
    }

    /**
     * 경로 요약은 {@code /list}에 없는 필드다 — 지도 화면만 거리·시간·경로선을 그린다.
     * {@code path}는 카카오가 평탄화해서 준 좌표열이 [경도, 위도] 쌍으로 묶여 나간 것이다.
     */
    @Test
    @DisplayName("경로 요약에 거리와 경로선이 함께 실린다")
    void routeSummary_carriesDistanceAndPath() {
        stubGeocoding();
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.routes[0].summary.distanceMeters", equalTo((int) ROUTE_DISTANCE_METERS))
                .body("data.routes[0].summary.path", hasSize(ROUTE_VERTEXES.length / 2))
                .body("data.routes[0].summary.path[0][0]", equalTo((float) ORIGIN_LONGITUDE))
                .body("data.routes[0].summary.path[0][1]", equalTo((float) ORIGIN_LATITUDE));
    }

    @Test
    @DisplayName("경로 반경 안에 휴게소가 없으면 실패가 아니라 빈 목록이 나간다")
    void noRestStopNearRoute_respondsEmptyRestStops() {
        saveOffRouteRestStop(restStopRepository);
        stubGeocoding();
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data.routes", hasSize(1))
                .body("data.routes[0].restStops", hasSize(0));
    }

    /**
     * 이 엔드포인트는 카카오 외에 국가 유가도 조회하는데, e2e 프로파일에서는 그 주소가 닫혀 있어
     * 항상 실패한다. 그런데도 200이 나가는 것이 <b>부분 실패를 견딘다</b>는 증거다 — 유가를
     * 못 얻으면 비교 값만 비고, 경로와 휴게소는 그대로 나간다.
     */
    @Test
    @DisplayName("국가 유가 조회가 실패해도 경로 휴게소 응답은 그대로 나간다")
    void nationalOilPriceUnavailable_doesNotBreakResponse() {
        saveOnRouteRestStop(restStopRepository);
        stubGeocoding();
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data.routes[0].restStops", hasSize(1))
                .body("data.routes[0].restStops[0].comparisonSummary.gasolinePrice", nullValue())
                .body("data.routes[0].restStops[0].fuelPriceTier", nullValue());
    }

    // --- 잘못된 요청 ---------------------------------------------------------

    @Test
    @DisplayName("출발지 좌표가 없으면 400이 나간다")
    void missingOrigin_responds400() {
        given().params(Map.of("originLng", ORIGIN_LONGITUDE, "destinationQuery", DESTINATION_QUERY))
                .when()
                .get(PATH)
                .then()
                .statusCode(BAD_REQUEST_STATUS)
                .body("code", equalTo("INVALID_PARAMETER"));
    }

    /**
     * 목적지를 지정하는 파라미터가 하나도 없어도 400이 아니다 — 빈 검색어로 지오코딩을 시도하고,
     * 카카오가 뭔가 돌려주면 그 좌표를 목적지로 삼아 성공까지 간다. 사용자 입력 검증이 외부 호출
     * 뒤로 밀려 쿼터와 지연을 먼저 쓰는 구조이며, 여기서는 그 사실을 고정만 해둔다.
     */
    @Test
    @DisplayName("목적지를 하나도 지정하지 않아도 걸러지지 않고 카카오까지 나간다")
    void noDestinationGiven_reachesExternalApi() {
        stubGeocoding();
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(Map.of("originLat", ORIGIN_LATITUDE, "originLng", ORIGIN_LONGITUDE))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data.destination.name", equalTo(GEOCODED_NAME));
    }

    // --- 비즈니스적 실패: 404 + 구체적 안내 문구 -------------------------------

    /** 목적지 해석에서 끝나므로 길찾기까지 가지 않는다 — 쓸데없는 외부 호출이 없다는 뜻. */
    @Test
    @DisplayName("장소 검색 결과가 없으면 길찾기를 부르지 않고 404가 나간다")
    void noPlaceSearchResult_responds404WithoutDirections() {
        stubKeywordSearchDocuments(KAKAO);

        get(destinationQueryParams("없는곳"))
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("목적지 검색 결과가 없습니다: 없는곳"));

        verifyDirectionsNotCalled(KAKAO);
    }

    @Test
    @DisplayName("길찾기가 경로를 못 찾으면 404와 함께 결과 코드별 안내 문구가 나간다")
    void routeNotFound_responds404WithGuidance() {
        stubGeocoding();
        stubDirectionsResultCode(KAKAO, 104);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("출발지와 도착지가 너무 가까워요. 좀 더 떨어진 위치를 선택해주세요."));
    }

    // --- 외부 API 실패: 어느 호출이 죽었는지 갈리지 않는다 ----------------------

    /** 지오코딩 단계에서 죽은 경우. 길찾기는 시작도 못 한다. */
    @Test
    @DisplayName("장소 검색이 HTTP 500이면 200 + EXTERNAL_API_UNAVAILABLE로 나간다")
    void keywordSearchServerError_respondsExternalApiUnavailable() {
        stubKeywordSearchStatus(KAKAO, 500);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE))
                .body("data", nullValue());

        verifyDirectionsNotCalled(KAKAO);
    }

    @Test
    @DisplayName("장소 검색 연결이 끊겨도 상태코드 실패와 같은 응답이 된다")
    void keywordSearchConnectionReset_respondsExternalApiUnavailable() {
        stubKeywordSearchConnectionReset(KAKAO);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    /**
     * 지오코딩은 성공하고 길찾기에서 죽은 경우. 위의 장소 검색 실패와 <b>응답이 똑같다</b> —
     * 사용자도 로그 밖의 우리도 어느 호출이 죽었는지 응답만 봐서는 알 수 없다는 뜻이다.
     */
    @Test
    @DisplayName("길찾기가 HTTP 500이어도 장소 검색 실패와 구분되지 않는다")
    void directionsServerError_isNotDistinguishedFromKeywordSearchFailure() {
        stubGeocoding();
        stubDirectionsStatus(KAKAO, 500);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE))
                .body("data", nullValue());

        verifyKeywordSearchCalled(KAKAO, DESTINATION_QUERY);
    }

    @Test
    @DisplayName("길찾기 연결이 끊겨도 같은 응답이 된다")
    void directionsConnectionReset_respondsExternalApiUnavailable() {
        stubGeocoding();
        stubDirectionsConnectionReset(KAKAO);

        get(destinationQueryParams(DESTINATION_QUERY))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }
}
