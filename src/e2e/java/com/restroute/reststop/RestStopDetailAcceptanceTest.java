package com.restroute.reststop;

import static com.restroute.support.RestStopFixtures.ROUTE_NAME;
import static com.restroute.support.RestStopFixtures.saveRestStop;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import com.restroute.reststop.repository.RestStopRepository;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 휴게소 상세 화면이 섹션별로 부르는 조회 7종을 한자리에 모은다. 화면 하나가 이 일곱을 병렬로
 * 불러 섹션을 채우므로, 서로 어긋나면 같은 휴게소인데 섹션마다 다른 결과가 나온다.
 *
 * <p>일곱은 컨트롤러 모양이 같다 — 서비스가 {@code Optional}을 주고 있으면 200, 없으면 404다.
 * 그래서 <b>휴게소가 없을 때 404</b>는 일곱이 동일하고 한 번에 검증한다.
 *
 * <p>반면 <b>휴게소는 있는데 연관 데이터가 없을 때</b>는 갈린다. 여섯은 200과 빈 값을 주는데
 * {@code oil-info}만 404다. 이 차이는 설계 의도가 아니라 관찰된 사실이며, 묶어서 검증하면
 * 지워지므로 따로 고정한다({@code harness/runs/current/rest-stop-endpoint-as-is.md} 참고).
 *
 * <p><b>이 계층이 필요한 이유:</b> {@code src/test}는 H2에서 돌고 운영은 MySQL이며,
 * {@code @WebMvcTest}가 3개뿐이라 대부분의 컨트롤러는 실제 JSON을 만들어보지 않는다.
 */
class RestStopDetailAcceptanceTest extends AcceptanceTest {

    private static final String BASE = "/api/rest-stops";
    private static final String UNKNOWN_CODE = "UNKNOWN";

    private static final int SUCCESS_STATUS = 200;
    private static final int NOT_FOUND_STATUS = 404;

    private static final String UNIT_NAME = "안성휴게소";
    private static final String LONGITUDE = "127.0000";
    private static final String LATITUDE = "37.5000";

    @Autowired
    private RestStopRepository restStopRepository;

    private Response get(String code, String section) {
        return given().when().get(BASE + "/" + code + section);
    }

    private String savedRestStop() {
        return saveRestStop(restStopRepository, UNIT_NAME, LONGITUDE, LATITUDE);
    }

    // --- 휴게소 자체가 없을 때: 일곱이 모두 같다 -------------------------------

    @ParameterizedTest(name = "GET /api/rest-stops/UNKNOWN{0}")
    @ValueSource(strings = {"", "/basic-info", "/facilities", "/foods", "/events", "/sales-rankings", "/oil-info"})
    @DisplayName("휴게소가 없으면 상세 조회 전부가 404와 NOT_FOUND로 나간다")
    void unknownServiceAreaCode_responds404(String section) {
        get(UNKNOWN_CODE, section)
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("data", nullValue());
    }

    // --- 휴게소는 있고 연관 데이터가 없을 때: 여섯은 빈 값 200 ------------------

    @Test
    @DisplayName("휴게소 기본 조회는 저장한 값을 그대로 돌려준다")
    void restStopDetail_carriesSavedValues() {
        String code = savedRestStop();

        get(code, "")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data.serviceAreaCode", equalTo(code))
                .body("data.unitName", equalTo(UNIT_NAME))
                .body("data.routeName", equalTo(ROUTE_NAME))
                .body("data.xValue", equalTo(LONGITUDE))
                .body("data.yValue", equalTo(LATITUDE));
    }

    /** 상세 정보가 아직 동기화되지 않은 휴게소. 화면은 기본 정보만으로도 열려야 한다. */
    @Test
    @DisplayName("상세 정보가 없어도 기본 정보 조회는 200과 빈 값으로 나간다")
    void basicInfoWithoutDetail_respondsEmptyFields() {
        String code = savedRestStop();

        get(code, "/basic-info")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.serviceAreaCode", equalTo(code))
                .body("data.unitName", equalTo(UNIT_NAME))
                .body("data.address", nullValue())
                .body("data.telNo", nullValue())
                .body("data.brand", nullValue())
                .body("data.evChargerCount", equalTo(0));
    }

    @Test
    @DisplayName("편의시설 정보가 없어도 200과 빈 값으로 나간다")
    void facilitiesWithoutData_respondsEmptyFields() {
        String code = savedRestStop();

        get(code, "/facilities")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.convenienceFacilities", hasSize(0))
                .body("data.hasMaintenance", nullValue())
                .body("data.maleToiletCount", nullValue());
    }

    @Test
    @DisplayName("먹거리가 없어도 200과 빈 목록으로 나간다")
    void foodsWithoutData_respondsEmptyLists() {
        String code = savedRestStop();

        get(code, "/foods")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.menus", hasSize(0))
                .body("data.sections", hasSize(0));
    }

    @Test
    @DisplayName("행사가 없어도 200과 빈 목록으로 나간다")
    void eventsWithoutData_respondsEmptyList() {
        String code = savedRestStop();

        get(code, "/events").then().statusCode(SUCCESS_STATUS).body("data.events", hasSize(0));
    }

    @Test
    @DisplayName("판매순위가 없어도 200과 빈 목록으로 나간다")
    void salesRankingsWithoutData_respondsEmptyLists() {
        String code = savedRestStop();

        get(code, "/sales-rankings")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.baseYearMonth", nullValue())
                .body("data.storeRankings", hasSize(0))
                .body("data.products", hasSize(0));
    }

    // --- 혼자 다른 계약 -------------------------------------------------------

    /**
     * 위 여섯과 갈리는 지점이다. 휴게소가 있어도 주유소 연결이 없으면 404이며, 프론트는 섹션별
     * 404를 "해당 없음"으로 처리해 화면이 깨지지는 않는다. 계약을 맞추려면 서비스를 바꿔야 하므로
     * 여기서는 현재 동작을 고정만 해둔다 — 바꿀 때 이 테스트가 빨간불로 알려준다.
     */
    @Test
    @DisplayName("주유 정보만은 휴게소가 있어도 연결이 없으면 404다")
    void oilInfoWithoutOilStation_responds404UnlikeOtherSections() {
        String code = savedRestStop();

        get(code, "/oil-info").then().statusCode(NOT_FOUND_STATUS).body("code", equalTo("NOT_FOUND"));

        // 같은 휴게소인데 다른 섹션은 200이다 — 이 대비가 이 테스트의 요점이다.
        get(code, "/foods").then().statusCode(SUCCESS_STATUS);
    }
}
