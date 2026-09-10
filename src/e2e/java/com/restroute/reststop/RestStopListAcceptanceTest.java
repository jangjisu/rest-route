package com.restroute.reststop;

import static com.restroute.support.RestStopFixtures.saveRestStop;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import com.restroute.reststop.repository.RestStopRepository;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 휴게소 목록·검색 3종({@code GET /api/rest-stops}, {@code /search}, {@code /nearby})을 한자리에 모은다.
 *
 * <p>엔드포인트당 한 파일 원칙의 예외다 — 셋이 같은 목록 성격이고 같은 픽스처(여러 휴게소를 심고
 * 어느 것이 걸러지거나 앞서는지)를 공유해서, 나누면 같은 데이터 배치를 세 번 반복하게 된다.
 *
 * <p><b>이 계층이 필요한 이유:</b> {@code src/test}의 통합 테스트는 전부 <b>H2</b>에서 돌고 운영은
 * <b>MySQL</b>이다. 검색과 정렬이 실제 MySQL에서 어떤 결과를 내는지는 여기서만 확인된다.
 *
 * <p>다만 두 DB가 갈리는 지점을 이 계층이 <b>전부</b> 잡아주지는 않는다 — 예를 들어 대소문자
 * 무시는 MySQL의 기본 collation이 알아서 해주므로, 리포지토리에서 {@code IgnoreCase}를 빼도
 * 여기서는 통과한다. 두 계층이 지키는 것이 서로 다르다.
 *
 * <p>기대값은 관찰된 현재 동작이다 — {@code harness/runs/current/rest-stop-endpoint-as-is.md} 참고.
 */
class RestStopListAcceptanceTest extends AcceptanceTest {

    private static final String PATH = "/api/rest-stops";
    private static final String SEARCH_PATH = PATH + "/search";
    private static final String NEARBY_PATH = PATH + "/nearby";

    private static final int SUCCESS_STATUS = 200;
    private static final int BAD_REQUEST_STATUS = 400;

    /** 서울시청 근처. nearby의 기준점으로 쓴다. */
    private static final double ORIGIN_LATITUDE = 37.5665;

    private static final double ORIGIN_LONGITUDE = 126.9780;

    @Autowired
    private RestStopRepository restStopRepository;

    private Response searchByName(String name) {
        return given().param("name", name).when().get(SEARCH_PATH);
    }

    private Response nearby(Map<String, Object> params) {
        return given().params(params).when().get(NEARBY_PATH);
    }

    /** 기준점에서 가까운 순서로 셋을 심는다 — 거리순 정렬이 실제로 돌았는지 보려면 순서가 뒤섞여야 한다. */
    private void saveThreeAtIncreasingDistance() {
        saveRestStop(restStopRepository, "먼휴게소", "127.5100", "37.8300");
        saveRestStop(restStopRepository, "가까운휴게소", "127.0000", "37.5600");
        saveRestStop(restStopRepository, "중간휴게소", "127.2000", "37.6500");
    }

    // --- 전체 목록 -----------------------------------------------------------

    @Test
    @DisplayName("저장된 휴게소가 없으면 실패가 아니라 빈 목록이 나간다")
    void noRestStops_respondsEmptyList() {
        given().when()
                .get(PATH)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(0));
    }

    @Test
    @DisplayName("저장된 휴게소를 목록으로 돌려준다")
    void savedRestStops_areListed() {
        saveRestStop(restStopRepository, "안성휴게소");
        saveRestStop(restStopRepository, "죽전휴게소");

        given().when()
                .get(PATH)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(2))
                .body("data.unitName", contains("안성휴게소", "죽전휴게소"));
    }

    // --- 이름 검색 -----------------------------------------------------------

    /** 부분 일치가 실제로 걸러내는지 보려면 안 걸리는 휴게소가 함께 있어야 한다. */
    @Test
    @DisplayName("이름 일부로 검색하면 그 이름을 포함한 휴게소만 나온다")
    void partialName_matchesOnlyContainingRestStops() {
        saveRestStop(restStopRepository, "안성휴게소");
        saveRestStop(restStopRepository, "죽전휴게소");

        searchByName("안성")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(1))
                .body("data[0].unitName", equalTo("안성휴게소"));
    }

    /**
     * 사용자가 소문자로 쳐도 대문자로 시작하는 이름이 잡히고, 한글 부분 일치도 같은 쿼리에서
     * 동작한다는 것을 고정한다.
     *
     * <p>다만 이 테스트가 {@code IgnoreCase}를 지키지는 <b>못한다</b> — 리포지토리에서 그
     * 키워드를 빼도 여기서는 통과한다. MySQL의 기본 collation이 이미 대소문자를 구분하지 않아서
     * {@code LIKE}가 알아서 무시하기 때문이다. 그 키워드를 지키는 것은 H2에서 도는 단위 테스트
     * 쪽이고, 여기서 고정하는 것은 <b>사용자에게 나가는 결과</b>다.
     */
    @Test
    @DisplayName("대소문자를 무시하고 검색하며 한글 부분 일치도 함께 동작한다")
    void search_ignoresCaseAndMatchesKorean() {
        saveRestStop(restStopRepository, "Bravo휴게소");
        saveRestStop(restStopRepository, "alpha휴게소");

        searchByName("bravo")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(1))
                .body("data[0].unitName", equalTo("Bravo휴게소"));

        searchByName("ALPHA")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(1))
                .body("data[0].unitName", equalTo("alpha휴게소"));

        searchByName("휴게소").then().statusCode(SUCCESS_STATUS).body("data", hasSize(2));
    }

    @Test
    @DisplayName("공백뿐인 검색어는 조회 없이 빈 목록이 나간다")
    void blankName_respondsEmptyList() {
        saveRestStop(restStopRepository, "안성휴게소");

        searchByName("   ").then().statusCode(SUCCESS_STATUS).body("data", hasSize(0));
    }

    @Test
    @DisplayName("name 파라미터가 없으면 400이 나간다")
    void missingNameParameter_responds400() {
        given().when()
                .get(SEARCH_PATH)
                .then()
                .statusCode(BAD_REQUEST_STATUS)
                .body("code", equalTo("INVALID_PARAMETER"));
    }

    // --- nearby --------------------------------------------------------------

    @Test
    @DisplayName("내 위치를 주면 거리를 실어 가까운 순서로 돌려준다")
    void withOrigin_sortsByDistanceAndCarriesIt() {
        saveThreeAtIncreasingDistance();

        nearby(Map.of("originLat", ORIGIN_LATITUDE, "originLng", ORIGIN_LONGITUDE))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(3))
                .body("data.unitName", contains("가까운휴게소", "중간휴게소", "먼휴게소"))
                .body("data[0].distanceMeters", notNullValue())
                .body("data[2].distanceMeters", greaterThan(0f));
    }

    /** 위치 권한을 안 준 사용자가 타는 경로. 거리 없이도 목록 자체는 나와야 한다. */
    @Test
    @DisplayName("내 위치가 없으면 거리 없이 전체를 돌려준다")
    void withoutOrigin_respondsWithoutDistance() {
        saveThreeAtIncreasingDistance();

        nearby(Map.of())
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(3))
                .body("data[0].distanceMeters", nullValue());
    }

    @Test
    @DisplayName("내 위치와 이름을 함께 주면 이름으로 거른 뒤 거리순으로 돌려준다")
    void withOriginAndName_filtersThenSorts() {
        saveThreeAtIncreasingDistance();
        saveRestStop(restStopRepository, "다른이름", "127.0100", "37.5610");

        nearby(Map.of(
                        "originLat", ORIGIN_LATITUDE,
                        "originLng", ORIGIN_LONGITUDE,
                        "name", "휴게소"))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(3))
                .body("data.unitName", contains("가까운휴게소", "중간휴게소", "먼휴게소"));
    }

    @Test
    @DisplayName("휴게소가 없으면 빈 목록이 나간다")
    void nearbyWithNoRestStops_respondsEmptyList() {
        nearby(Map.of("originLat", ORIGIN_LATITUDE, "originLng", ORIGIN_LONGITUDE))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(0));
    }
}
