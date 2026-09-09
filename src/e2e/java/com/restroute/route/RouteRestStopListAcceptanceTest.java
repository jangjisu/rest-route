package com.restroute.route;

import static com.restroute.support.KakaoApiStubs.stubDirections;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearch;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.restroute.AcceptanceTest;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * finder "목적지로 추천받기"가 쓰는 {@code GET /api/route-rest-stops/list}의 배선을 검증한다.
 *
 * <p>반경·상하행·좌표축소 같은 <b>규칙</b>은 이미 단위 테스트가 덮고 있다
 * ({@code RouteRestStopMatcherTest} 8개, {@code RouteCoordinateReducerTest} 8개,
 * {@code RouteRestStopListQueryServiceTest} 7개). 여기서 볼 것은 그 부품들이 실제로
 * <b>연결되어</b> 있는가다 — 요청이 컨트롤러에 닿고, 외부로 HTTP가 나가고, 카카오 JSON이
 * 레코드로 파싱되고, DB 조회를 거쳐 {@code ApiResponse}로 돌아오는 전 구간.
 *
 * <p>그래서 케이스를 늘리지 않는다. 조건 조합은 단위 테스트의 몫이고, 여기서 반복하면
 * 느려지기만 할 뿐 아니라 빨간불일 때 어느 구간이 깨졌는지 알 수 없게 된다.
 */
class RouteRestStopListAcceptanceTest extends AcceptanceTest {

    private static final double ORIGIN_LONGITUDE = 126.9780;
    private static final double ORIGIN_LATITUDE = 37.5665;

    /** 가짜 카카오가 돌려줄 경로. [경도, 위도] 쌍 3개짜리 짧은 직선이다. */
    private static final double[] ROUTE_VERTEXES = {
        ORIGIN_LONGITUDE, ORIGIN_LATITUDE, 126.9880, 37.5565, 126.9980, 37.5465
    };

    private static final String ON_ROUTE_NAME = "경로위휴게소";
    private static final String OFF_ROUTE_NAME = "경로밖휴게소";

    @Autowired
    private RestStopRepository restStopRepository;

    @Test
    @DisplayName("경로 반경 안의 휴게소만 담아 돌려준다")
    void restStopsWithinRadius_areReturned() {
        // 경로의 두 번째 정점 위에 두어 거리 0m — 반드시 포함되어야 한다.
        saveRestStop(ON_ROUTE_NAME, "126.9880", "37.5565");
        // 경로에서 수십 km 떨어뜨린다 — 반드시 빠져야 한다. 이 휴게소가 빠지는 것이
        // 반경 필터가 실제로 돌았다는 증거다(하나만 두면 필터가 고장나도 통과한다).
        saveRestStop(OFF_ROUTE_NAME, "127.5000", "37.9000");

        stubKeywordSearch(KAKAO, "부산역", 129.0403, 35.1148);
        stubDirections(KAKAO, 1_500L, ROUTE_VERTEXES);

        given().param("originLat", ORIGIN_LATITUDE)
                .param("originLng", ORIGIN_LONGITUDE)
                .param("destinationQuery", "부산역")
                .when()
                .get("/api/route-rest-stops/list")
                .then()
                .statusCode(200)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(1))
                .body("data[0].unitName", equalTo(ON_ROUTE_NAME));
    }

    /**
     * 이름을 서로 다르게 짓는 건 의도적이다 — 같은 이름의 상·하행 페어가 잡히면
     * {@code RouteRestStopMatcher}의 진행방향 판정까지 끌려들어와, 실패했을 때 원인이
     * 배선인지 방향 로직인지 갈라내기 어려워진다.
     */
    private void saveRestStop(String unitName, String longitude, String latitude) {
        restStopRepository.save(RestStopEntity.createByAdmin(unitName, "1", "경부고속도로", longitude, latitude));
    }
}
