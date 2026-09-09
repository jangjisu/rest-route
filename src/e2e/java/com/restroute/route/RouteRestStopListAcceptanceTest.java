package com.restroute.route;

import static com.restroute.support.KakaoApiStubs.stubDirections;
import static com.restroute.support.KakaoApiStubs.stubDirectionsResultCode;
import static com.restroute.support.KakaoApiStubs.stubDirectionsStatus;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearch;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchConnectionReset;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchDelay;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchEmpty;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchNonJson;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchStatus;
import static com.restroute.support.KakaoApiStubs.verifyKeywordSearchNotCalled;
import static com.restroute.support.RouteFixtures.DESTINATION_LATITUDE;
import static com.restroute.support.RouteFixtures.DESTINATION_LONGITUDE;
import static com.restroute.support.RouteFixtures.DESTINATION_QUERY;
import static com.restroute.support.RouteFixtures.ON_ROUTE_NAME;
import static com.restroute.support.RouteFixtures.ROUTE_DISTANCE_METERS;
import static com.restroute.support.RouteFixtures.ROUTE_VERTEXES;
import static com.restroute.support.RouteFixtures.saveOffRouteRestStop;
import static com.restroute.support.RouteFixtures.saveOnRouteRestStop;
import static com.restroute.support.RouteRestStopListApi.getByDestinationCoordinates;
import static com.restroute.support.RouteRestStopListApi.getByDestinationQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import com.restroute.reststop.repository.RestStopRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * finder "목적지로 추천받기"가 쓰는 {@code GET /api/route-rest-stops/list}가 카카오의 응답
 * 형태에 따라 사용자에게 무엇을 돌려주는지를 한자리에 모은다.
 *
 * <p>반경·상하행·좌표축소 같은 <b>규칙</b>은 단위 테스트 23개가 이미 덮고 있다
 * ({@code RouteRestStopMatcherTest}, {@code RouteCoordinateReducerTest},
 * {@code RouteRestStopListQueryServiceTest}). 여기서 보는 것은 그 부품들이 실제로
 * <b>연결되어</b> 돌아가는가, 그리고 <b>외부가 실패할 때 무엇이 사용자에게 나가는가</b>다.
 *
 * <p>실패 케이스의 기대값은 설계 의도가 아니라 <b>2026-09-09에 관찰된 현재 동작</b>이다 —
 * 기대값을 먼저 상상해 박아두면 "지금 이렇다"가 아니라 "이러면 좋겠다"를 검증하게 된다.
 * 회복탄력성 작업으로 동작을 바꾸면 이 테스트들이 빨간불이 되면서 무엇이 달라졌는지 드러낸다.
 * 관찰 결과 전체는 {@code harness/runs/current/external-api-failure-as-is.md} 참고.
 */
class RouteRestStopListAcceptanceTest extends AcceptanceTest {

    private static final String EXTERNAL_API_UNAVAILABLE = "EXTERNAL_API_UNAVAILABLE";
    private static final int SUCCESS_STATUS = 200;
    private static final int NOT_FOUND_STATUS = 404;

    /** 기본 readTimeout. 이 값을 넘겨야 끊기는지 보려고 지연을 이보다 길게 준다. */
    private static final int READ_TIMEOUT_MILLIS = 10_000;

    private static final int DELAY_OVER_TIMEOUT_MILLIS = 15_000;

    @Autowired
    private RestStopRepository restStopRepository;

    // --- 정상 ---------------------------------------------------------------

    @Test
    @DisplayName("경로 반경 안의 휴게소만 담아 돌려준다")
    void restStopsWithinRadius_areReturned() {
        saveOnRouteRestStop(restStopRepository);
        // 경로 밖 휴게소가 빠지는 것이 반경 필터가 실제로 돌았다는 증거다.
        // 하나만 두면 필터가 고장나도 통과해버려 아무것도 증명하지 못한다.
        saveOffRouteRestStop(restStopRepository);

        stubKeywordSearch(KAKAO, DESTINATION_QUERY, DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        getByDestinationQuery()
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(1))
                .body("data[0].unitName", equalTo(ON_ROUTE_NAME));
    }

    /**
     * 검색창에 직접 입력하고 후보를 골랐을 때 타는 경로. 프론트가 {@code /api/place-search}에서
     * 이미 좌표를 받아둔 상태라 서버는 "그 이름이 어디인지" 다시 물을 필요가 없다 — 지오코딩을
     * 건너뛰고 길찾기만 부르므로 카카오 호출이 2회가 아니라 1회다.
     *
     * <p>목적지 칩(부산역·대전역 등)은 이쪽이 아니라 위의 검색어 방식을 쓴다. 칩이 들고 있는
     * 건 좌표가 아니라 검색어뿐이다({@code finder-destination-chips.js}).
     */
    @Test
    @DisplayName("목적지 좌표를 직접 주면 지오코딩을 거치지 않는다")
    void destinationCoordinatesGiven_skipsGeocoding() {
        saveOnRouteRestStop(restStopRepository);
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        getByDestinationCoordinates().then().statusCode(SUCCESS_STATUS).body("data", hasSize(1));

        verifyKeywordSearchNotCalled(KAKAO);
    }

    /** 휴게소가 없는 지역으로 검색하면 나오는 결과. 실패가 아니라 빈 목록이다. */
    @Test
    @DisplayName("경로 반경 안에 휴게소가 없으면 실패가 아니라 빈 목록이 나간다")
    void noRestStopNearRoute_respondsEmptyList() {
        saveOffRouteRestStop(restStopRepository);

        stubKeywordSearch(KAKAO, DESTINATION_QUERY, DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        getByDestinationQuery()
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(0));
    }

    // --- 비즈니스적 실패: 404 + 구체적 안내 문구 -------------------------------

    @Test
    @DisplayName("목적지를 못 찾으면 404와 함께 무엇을 못 찾았는지 알려준다")
    void destinationNotFound_responds404WithReason() {
        stubKeywordSearchEmpty(KAKAO);

        getByDestinationQuery()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("목적지 검색 결과가 없습니다: 부산역"));
    }

    @Test
    @DisplayName("길찾기가 경로를 못 찾으면 404와 함께 결과 코드별 안내 문구가 나간다")
    void routeNotFound_responds404WithGuidance() {
        stubKeywordSearch(KAKAO, DESTINATION_QUERY, DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
        stubDirectionsResultCode(KAKAO, 104);

        getByDestinationQuery()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("출발지와 도착지가 너무 가까워요. 좀 더 떨어진 위치를 선택해주세요."));
    }

    /** 산·바다 한가운데서 GPS가 잡히면 실제로 나오는 상황. 사용자는 출발지를 옮겨야 한다. */
    @Test
    @DisplayName("출발지 주변에 도로가 없으면 출발지를 바꾸라고 안내한다")
    void originHasNoNearbyRoad_responds404WithOriginGuidance() {
        stubKeywordSearch(KAKAO, DESTINATION_QUERY, DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
        stubDirectionsResultCode(KAKAO, 101);

        getByDestinationQuery()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("message", equalTo("출발지 주변에서 도로를 찾지 못했어요. 출발지를 도로에 가까운 위치로 바꿔주세요."));
    }

    /** 위와 원인이 갈리는 지점 — 사용자가 고쳐야 할 대상이 출발지가 아니라 도착지다. */
    @Test
    @DisplayName("도착지 주변에 도로가 없으면 도착지를 바꾸라고 안내한다")
    void destinationHasNoNearbyRoad_responds404WithDestinationGuidance() {
        stubKeywordSearch(KAKAO, DESTINATION_QUERY, DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
        stubDirectionsResultCode(KAKAO, 102);

        getByDestinationQuery()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("message", equalTo("도착지 주변에서 도로를 찾지 못했어요. 도착지를 도로에 가까운 위치로 바꿔주세요."));
    }

    /** 카카오가 우리가 모르는 코드를 새로 내보내도 안내 없이 끝나지 않는다. */
    @Test
    @DisplayName("모르는 결과 코드가 와도 기본 안내 문구로 떨어진다")
    void unknownRouteResultCode_respondsDefaultGuidance() {
        stubKeywordSearch(KAKAO, DESTINATION_QUERY, DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
        stubDirectionsResultCode(KAKAO, 999);

        getByDestinationQuery()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("message", equalTo("경로를 찾지 못했어요. 출발지와 도착지를 다시 확인해주세요."));
    }

    // --- 외부 API 실패: 원인과 무관하게 200 + EXTERNAL_API_UNAVAILABLE ---------

    @Test
    @DisplayName("지오코딩이 HTTP 500이면 200 + EXTERNAL_API_UNAVAILABLE로 나간다")
    void geocodingServerError_respondsExternalApiUnavailable() {
        stubKeywordSearchStatus(KAKAO, 500);

        getByDestinationQuery()
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE))
                .body("data", nullValue());
    }

    /** 429는 백오프가, 500은 재시도가 맞는 대응인데 지금은 갈리지 않는다는 것을 고정한다. */
    @Test
    @DisplayName("지오코딩이 HTTP 429여도 서버 장애와 똑같이 처리된다")
    void geocodingQuotaExceeded_isNotDistinguishedFromServerError() {
        stubKeywordSearchStatus(KAKAO, 429);

        getByDestinationQuery().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    @Test
    @DisplayName("에러 응답 본문이 JSON이 아니어도 파싱 실패로 새지 않는다")
    void geocodingNonJsonErrorBody_respondsExternalApiUnavailable() {
        stubKeywordSearchNonJson(KAKAO, 503);

        getByDestinationQuery().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    /** 상태코드조차 못 받는 갈래 — 서버가 내려갔거나 중간 네트워크가 끊긴 상황. */
    @Test
    @DisplayName("연결이 끊겨도 상태코드 실패와 같은 응답이 된다")
    void connectionReset_respondsExternalApiUnavailable() {
        stubKeywordSearchConnectionReset(KAKAO);

        getByDestinationQuery().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    @Test
    @DisplayName("길찾기가 HTTP 500이면 체인 뒷단 실패도 같은 응답이 된다")
    void directionsServerError_respondsExternalApiUnavailable() {
        stubKeywordSearch(KAKAO, DESTINATION_QUERY, DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
        stubDirectionsStatus(KAKAO, 500);

        getByDestinationQuery().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    // --- 타임아웃 -----------------------------------------------------------

    /**
     * 이 테스트만 느리다(10초). 그래도 남기는 건, 설정한 readTimeout이 실제로 걸리는지는
     * 클라이언트를 목으로 바꿔치기하는 단위 테스트로는 확인할 방법이 없어서다.
     *
     * <p>동시에 두 가지를 기록한다 — 재시도가 없다는 것(있었다면 20초·30초가 됐을 것),
     * 그리고 사용자가 <b>실패를 알기까지 10초를 기다린다</b>는 것.
     */
    @Test
    @DisplayName("지오코딩이 늦으면 readTimeout에서 끊기고 재시도 없이 한 번만 시도한다")
    void geocodingSlowerThanReadTimeout_failsOnceAfterTimeout() {
        stubKeywordSearchDelay(KAKAO, DELAY_OVER_TIMEOUT_MILLIS);

        Instant startedAt = Instant.now();
        getByDestinationQuery().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();

        assertThat(elapsedMillis)
                .as("readTimeout에서 끊겼다면 10초를 갓 넘겨야 하고, 재시도가 붙었다면 그 배수가 된다")
                .isBetween((long) READ_TIMEOUT_MILLIS, (long) DELAY_OVER_TIMEOUT_MILLIS);
    }
}
