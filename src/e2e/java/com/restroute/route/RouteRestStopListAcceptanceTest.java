package com.restroute.route;

import static com.restroute.support.KakaoApiStubs.stubDirections;
import static com.restroute.support.KakaoApiStubs.stubDirectionsConnectionReset;
import static com.restroute.support.KakaoApiStubs.stubDirectionsDelay;
import static com.restroute.support.KakaoApiStubs.stubDirectionsNonJson;
import static com.restroute.support.KakaoApiStubs.stubDirectionsResultCode;
import static com.restroute.support.KakaoApiStubs.stubDirectionsStatus;
import static com.restroute.support.KakaoApiStubs.verifyKeywordSearchNotCalled;
import static com.restroute.support.RouteFixtures.ON_ROUTE_NAME;
import static com.restroute.support.RouteFixtures.ROUTE_DISTANCE_METERS;
import static com.restroute.support.RouteFixtures.ROUTE_VERTEXES;
import static com.restroute.support.RouteFixtures.UNKNOWN_DESTINATION_NAME;
import static com.restroute.support.RouteFixtures.saveOffRouteRestStop;
import static com.restroute.support.RouteFixtures.saveOnRouteRestStop;
import static com.restroute.support.RouteRestStopListApi.getByDestinationCoordinates;
import static com.restroute.support.RouteRestStopListApi.getByDestinationName;
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
 * <p>이 API는 <b>지오코딩을 타지 않는다</b>. 목적지를 이름으로 넘기면 서버가 알고 있는 목적지
 * 목록에서 좌표를 찾고, 좌표로 넘기면 그대로 쓴다. 그래서 외부 호출은 길찾기 <b>1회</b>뿐이고,
 * 카카오 로컬(장소 검색)은 이 경로에 관여하지 않는다 — 그건 별도 엔드포인트
 * {@code /api/place-search}의 몫이다.
 *
 * <p>반경·상하행·좌표축소 같은 <b>규칙</b>은 단위 테스트 23개가 이미 덮고 있다
 * ({@code RouteRestStopMatcherTest}, {@code RouteCoordinateReducerTest},
 * {@code RouteRestStopListQueryServiceTest}). 여기서 보는 것은 그 부품들이 실제로
 * <b>연결되어</b> 돌아가는가, 그리고 <b>외부가 실패할 때 무엇이 사용자에게 나가는가</b>다.
 *
 * <p>실패 케이스의 기대값은 설계 의도가 아니라 <b>관찰된 현재 동작</b>이다 — 회복탄력성 작업으로
 * 동작을 바꾸면 이 테스트들이 빨간불이 되면서 무엇이 달라졌는지 드러낸다. 관찰 결과 전체는
 * {@code harness/runs/current/external-api-failure-as-is.md} 참고.
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

    /** 목적지 칩이 타는 방식. 서버가 이름만으로 좌표를 알아야 하므로 지오코딩이 필요 없다. */
    @Test
    @DisplayName("목적지 이름만 줘도 지오코딩 없이 경로 반경 안의 휴게소를 돌려준다")
    void destinationName_resolvesWithoutGeocoding() {
        saveOnRouteRestStop(restStopRepository);
        // 경로 밖 휴게소가 빠지는 것이 반경 필터가 실제로 돌았다는 증거다.
        // 하나만 두면 필터가 고장나도 통과해버려 아무것도 증명하지 못한다.
        saveOffRouteRestStop(restStopRepository);

        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        getByDestinationName()
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(1))
                .body("data[0].unitName", equalTo(ON_ROUTE_NAME));

        verifyKeywordSearchNotCalled(KAKAO);
    }

    /** 검색창에 직접 입력하고 후보를 골랐을 때 타는 방식. 프론트가 이미 좌표를 들고 있다. */
    @Test
    @DisplayName("목적지 좌표를 직접 줘도 지오코딩 없이 같은 결과를 돌려준다")
    void destinationCoordinates_resolveWithoutGeocoding() {
        saveOnRouteRestStop(restStopRepository);
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        getByDestinationCoordinates()
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(1))
                .body("data[0].unitName", equalTo(ON_ROUTE_NAME));

        verifyKeywordSearchNotCalled(KAKAO);
    }

    /** 휴게소가 없는 지역으로 검색하면 나오는 결과. 실패가 아니라 빈 목록이다. */
    @Test
    @DisplayName("경로 반경 안에 휴게소가 없으면 실패가 아니라 빈 목록이 나간다")
    void noRestStopNearRoute_respondsEmptyList() {
        saveOffRouteRestStop(restStopRepository);
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        getByDestinationName()
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(0));
    }

    // --- 비즈니스적 실패: 404 + 구체적 안내 문구 -------------------------------

    /** 서버가 좌표를 모르는 이름이면 길찾기까지 가지 못한다 — 카카오를 아예 부르지 않는다. */
    @Test
    @DisplayName("서버가 좌표를 모르는 목적지 이름이면 404가 나간다")
    void unknownDestinationName_responds404() {
        getByDestinationName(UNKNOWN_DESTINATION_NAME)
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("목적지 검색 결과가 없습니다: " + UNKNOWN_DESTINATION_NAME));

        verifyKeywordSearchNotCalled(KAKAO);
    }

    @Test
    @DisplayName("길찾기가 경로를 못 찾으면 404와 함께 결과 코드별 안내 문구가 나간다")
    void routeNotFound_responds404WithGuidance() {
        stubDirectionsResultCode(KAKAO, 104);

        getByDestinationName()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("출발지와 도착지가 너무 가까워요. 좀 더 떨어진 위치를 선택해주세요."));
    }

    /** 산·바다 한가운데서 GPS가 잡히면 실제로 나오는 상황. 사용자는 출발지를 옮겨야 한다. */
    @Test
    @DisplayName("출발지 주변에 도로가 없으면 출발지를 바꾸라고 안내한다")
    void originHasNoNearbyRoad_responds404WithOriginGuidance() {
        stubDirectionsResultCode(KAKAO, 101);

        getByDestinationName()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("message", equalTo("출발지 주변에서 도로를 찾지 못했어요. 출발지를 도로에 가까운 위치로 바꿔주세요."));
    }

    /** 위와 원인이 갈리는 지점 — 사용자가 고쳐야 할 대상이 출발지가 아니라 도착지다. */
    @Test
    @DisplayName("도착지 주변에 도로가 없으면 도착지를 바꾸라고 안내한다")
    void destinationHasNoNearbyRoad_responds404WithDestinationGuidance() {
        stubDirectionsResultCode(KAKAO, 102);

        getByDestinationName()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("message", equalTo("도착지 주변에서 도로를 찾지 못했어요. 도착지를 도로에 가까운 위치로 바꿔주세요."));
    }

    /** 카카오가 우리가 모르는 코드를 새로 내보내도 안내 없이 끝나지 않는다. */
    @Test
    @DisplayName("모르는 결과 코드가 와도 기본 안내 문구로 떨어진다")
    void unknownRouteResultCode_respondsDefaultGuidance() {
        stubDirectionsResultCode(KAKAO, 999);

        getByDestinationName()
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("message", equalTo("경로를 찾지 못했어요. 출발지와 도착지를 다시 확인해주세요."));
    }

    // --- 외부 API 실패: 원인과 무관하게 200 + EXTERNAL_API_UNAVAILABLE ---------

    @Test
    @DisplayName("길찾기가 HTTP 500이면 200 + EXTERNAL_API_UNAVAILABLE로 나간다")
    void directionsServerError_respondsExternalApiUnavailable() {
        stubDirectionsStatus(KAKAO, 500);

        getByDestinationName()
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE))
                .body("data", nullValue());
    }

    /** 429는 백오프가, 500은 재시도가 맞는 대응인데 지금은 갈리지 않는다는 것을 고정한다. */
    @Test
    @DisplayName("길찾기가 HTTP 429여도 서버 장애와 똑같이 처리된다")
    void directionsQuotaExceeded_isNotDistinguishedFromServerError() {
        stubDirectionsStatus(KAKAO, 429);

        getByDestinationName().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    @Test
    @DisplayName("에러 응답 본문이 JSON이 아니어도 파싱 실패로 새지 않는다")
    void directionsNonJsonErrorBody_respondsExternalApiUnavailable() {
        stubDirectionsNonJson(KAKAO, 503);

        getByDestinationName().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    /** 상태코드조차 못 받는 갈래 — 서버가 내려갔거나 중간 네트워크가 끊긴 상황. */
    @Test
    @DisplayName("연결이 끊겨도 상태코드 실패와 같은 응답이 된다")
    void directionsConnectionReset_respondsExternalApiUnavailable() {
        stubDirectionsConnectionReset(KAKAO);

        getByDestinationName().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
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
    @DisplayName("길찾기가 늦으면 readTimeout에서 끊기고 재시도 없이 한 번만 시도한다")
    void directionsSlowerThanReadTimeout_failsOnceAfterTimeout() {
        stubDirectionsDelay(KAKAO, DELAY_OVER_TIMEOUT_MILLIS);

        Instant startedAt = Instant.now();
        getByDestinationName().then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();

        assertThat(elapsedMillis)
                .as("readTimeout에서 끊겼다면 10초를 갓 넘겨야 하고, 재시도가 붙었다면 그 배수가 된다")
                .isBetween((long) READ_TIMEOUT_MILLIS, (long) DELAY_OVER_TIMEOUT_MILLIS);
    }
}
