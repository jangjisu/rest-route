package com.restroute.reststop;

import static com.restroute.support.CsrfTokens.authorizedRequest;
import static com.restroute.support.ExApiStubs.stubOilPrice;
import static com.restroute.support.ExApiStubs.stubOilPriceConnectionReset;
import static com.restroute.support.ExApiStubs.stubOilPriceEmpty;
import static com.restroute.support.ExApiStubs.stubOilPriceStatus;
import static com.restroute.support.ExApiStubs.verifyOilPriceNotCalled;
import static com.restroute.support.RestStopFixtures.saveLinkedOilStation;
import static com.restroute.support.RestStopFixtures.saveRestStop;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import com.restroute.oilprice.repository.RestOilRepository;
import com.restroute.reststop.repository.RestStopRepository;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 휴게소 상세 화면의 "유가 새로고침"이 쓰는 {@code POST /api/rest-stops/{code}/oil-price/refresh}.
 *
 * <p>휴게소 공개 API 13개 중 <b>요청 시점에 외부 API를 부르는 유일한 엔드포인트</b>이고, 동시에
 * <b>유일한 변경 요청(POST)</b>이다. 그래서 두 가지가 여기서만 검증된다.
 *
 * <ul>
 *   <li>CSRF — {@code SecurityConfig}가 {@code /api/**}를 열어두면서도 CSRF는 끄지 않아서,
 *       토큰 없는 POST는 403이다. 프론트는 페이지의 {@code <meta>}에서 토큰을 읽어 싣는다.
 *       그 흐름을 통째로 재현하는 것은 이 계층 말고는 방법이 없다.
 *   <li>외부 API 실패 — 유가 제공자가 죽었을 때 사용자에게 무엇이 나가는가.
 * </ul>
 *
 * <p>기대값은 관찰된 현재 동작이다 — {@code harness/runs/current/rest-stop-endpoint-as-is.md} 참고.
 */
class RestStopOilPriceRefreshAcceptanceTest extends AcceptanceTest {

    private static final String BASE = "/api/rest-stops";
    private static final String REFRESH_SECTION = "/oil-price/refresh";

    private static final int SUCCESS_STATUS = 200;
    private static final int FORBIDDEN_STATUS = 403;
    private static final int NOT_FOUND_STATUS = 404;

    private static final String EXTERNAL_API_UNAVAILABLE = "EXTERNAL_API_UNAVAILABLE";

    /** 주유소 연결 코드. 외부 API에는 이 값이 조회 키로 나간다. */
    private static final String OIL_STATION_CODE = "000054";

    private static final String GASOLINE_PRICE = "1650";
    private static final String DIESEL_PRICE = "1520";
    private static final String LPG_PRICE = "1100";

    @Autowired
    private RestStopRepository restStopRepository;

    @Autowired
    private RestOilRepository restOilRepository;

    private Response refresh(String code) {
        return authorizedRequest().when().post(BASE + "/" + code + REFRESH_SECTION);
    }

    private String restStopWithOilStation() {
        String code = saveRestStop(restStopRepository, "안성휴게소");
        saveLinkedOilStation(restOilRepository, code, OIL_STATION_CODE);
        return code;
    }

    // --- CSRF ---------------------------------------------------------------

    /**
     * 토큰 없이 POST하면 막힌다. 본문이 {@code ApiResponse}가 아니라 스프링 기본 오류 형식인 것도
     * 함께 고정한다 — 필터 단계에서 잘리므로 우리 예외 처리기를 타지 않는다는 뜻이다.
     */
    @Test
    @DisplayName("CSRF 토큰 없이 POST하면 403으로 막힌다")
    void withoutCsrfToken_responds403() {
        String code = restStopWithOilStation();
        stubOilPrice(EX_API, OIL_STATION_CODE, GASOLINE_PRICE, DIESEL_PRICE, LPG_PRICE);

        given().when()
                .post(BASE + "/" + code + REFRESH_SECTION)
                .then()
                .statusCode(FORBIDDEN_STATUS)
                .body("error", equalTo("Forbidden"));

        verifyOilPriceNotCalled(EX_API);
    }

    // --- 정상 ---------------------------------------------------------------

    /**
     * 경유 가격만 외부 JSON의 이름이 {@code diselPrice}로 다르다(공공 API 원본의 철자다).
     * 클라이언트를 목으로 바꾸는 테스트에서는 이 매핑이 실행되지 않는다.
     */
    @Test
    @DisplayName("토큰을 실어 보내면 외부에서 받은 유가가 응답에 실린다")
    void withCsrfToken_respondsRefreshedPrices() {
        String code = restStopWithOilStation();
        stubOilPrice(EX_API, OIL_STATION_CODE, GASOLINE_PRICE, DIESEL_PRICE, LPG_PRICE);

        refresh(code)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data.gasolinePrice", equalTo(GASOLINE_PRICE))
                .body("data.dieselPrice", equalTo(DIESEL_PRICE))
                .body("data.lpgPrice", equalTo(LPG_PRICE));
    }

    // --- 외부에 나가기 전에 끝나는 갈래 ----------------------------------------

    @Test
    @DisplayName("휴게소가 없으면 외부를 부르지 않고 404가 나간다")
    void unknownRestStop_responds404WithoutCallingExternalApi() {
        refresh("UNKNOWN").then().statusCode(NOT_FOUND_STATUS).body("code", equalTo("NOT_FOUND"));

        verifyOilPriceNotCalled(EX_API);
    }

    /** 주유소가 연결되지 않은 휴게소는 조회 키가 없어 외부까지 갈 수 없다. */
    @Test
    @DisplayName("주유소 연결이 없으면 외부를 부르지 않고 404가 나간다")
    void restStopWithoutOilStation_responds404WithoutCallingExternalApi() {
        String code = saveRestStop(restStopRepository, "주유소없는휴게소");

        refresh(code).then().statusCode(NOT_FOUND_STATUS).body("code", equalTo("NOT_FOUND"));

        verifyOilPriceNotCalled(EX_API);
    }

    // --- 외부 API 실패 -------------------------------------------------------

    @Test
    @DisplayName("외부 유가 조회가 HTTP 500이면 200 + EXTERNAL_API_UNAVAILABLE로 나간다")
    void externalServerError_respondsExternalApiUnavailable() {
        String code = restStopWithOilStation();
        stubOilPriceStatus(EX_API, 500);

        refresh(code)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE))
                .body("data", nullValue());
    }

    @Test
    @DisplayName("외부 연결이 끊겨도 상태코드 실패와 같은 응답이 된다")
    void externalConnectionReset_respondsExternalApiUnavailable() {
        String code = restStopWithOilStation();
        stubOilPriceConnectionReset(EX_API);

        refresh(code).then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    /**
     * 조회 자체는 성공했는데 결과가 빈 갈래. <b>외부 실패와 응답이 갈린다</b> — 위 둘은 200과
     * EXTERNAL_API_UNAVAILABLE인데 이쪽은 404다. 화면에서 "지금은 유가를 못 가져왔다"와
     * "이 휴게소엔 유가가 없다"가 다른 안내로 이어지므로 갈리는 편이 맞고, 그 사실을 고정한다.
     */
    @Test
    @DisplayName("외부 조회 결과가 비면 외부 실패와 달리 404가 나간다")
    void externalEmptyResult_responds404UnlikeExternalFailure() {
        String code = restStopWithOilStation();
        stubOilPriceEmpty(EX_API);

        refresh(code).then().statusCode(NOT_FOUND_STATUS).body("code", equalTo("NOT_FOUND"));
    }
}
