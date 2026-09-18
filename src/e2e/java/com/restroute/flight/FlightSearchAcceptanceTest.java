package com.restroute.flight;

import static com.restroute.support.TravelpayoutsApiStubs.stubGroupedPrices;
import static com.restroute.support.TravelpayoutsApiStubs.stubGroupedPricesConnectionReset;
import static com.restroute.support.TravelpayoutsApiStubs.stubGroupedPricesEmpty;
import static com.restroute.support.TravelpayoutsApiStubs.stubGroupedPricesStatus;
import static com.restroute.support.TravelpayoutsApiStubs.stubGroupedPricesUnsuccessful;
import static com.restroute.support.TravelpayoutsApiStubs.verifyGroupedPricesNotCalled;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code GET /api/flights/search} — FIXED 모드, destination 하나만 지정한 가장 단순한
 * 요청으로 실 Travelpayouts 연동 한 층을 확인한다.
 *
 * <p>FIXED + 직접 destination 지정이면 {@code FlightFixedCallPlanner}가 정확히 <b>1번</b>만
 * Travelpayouts를 부른다({@code FlightSearchDestinations.resolve()}가 destination 하나를
 * 그대로 목록으로 돌려주고, sector가 아니라 "전체" 조회도 안 붙는다) — 그래서 병렬 팬아웃
 * 조율 로직 없이 껍데기(HTTP·JSON·예외 매핑)만 검증하면 된다. 팬아웃 자체는 단위 테스트가
 * 이미 덮는다.
 *
 * <p>날짜는 항상 {@link LocalDate#now()} 기준 상대값을 쓴다 — {@code dateTo}가 오늘로부터
 * 3개월을 넘으면 {@code date_range_too_wide}로 막히므로(FlightSearchRequestValidator),
 * 하드코딩한 날짜는 시간이 지나면 깨진다.
 *
 * <p>기대값은 실제 코드({@code FlightExceptionHandler}, {@code TravelpayoutsClient})를 읽고
 * 확정한 것이다 — 외부 API 실패는 house convention대로 <b>HTTP 200 +
 * {@code external_api_unavailable}</b>로 나간다.
 */
class FlightSearchAcceptanceTest extends AcceptanceTest {

    private static final String ENDPOINT = "/api/flights/search";
    private static final String ORIGIN = "ICN";
    private static final String DESTINATION = "NRT";

    @Test
    @DisplayName("Travelpayouts 조회에 성공하면 딜 목록을 담아 200을 응답한다")
    void success_respondsDealList() {
        stubGroupedPrices(
                TRAVELPAYOUTS,
                DESTINATION,
                250_000,
                departureAt() + "T09:00:00+09:00",
                returnAt() + "T11:00:00+09:00",
                "KE001");

        fixedSearchRequest()
                .when()
                .get(ENDPOINT)
                .then()
                .statusCode(200)
                .body("error", nullValue())
                .body("data", hasSize(1))
                .body("data[0].destination.code", equalTo(DESTINATION))
                .body("data[0].price.amount", equalTo(250_000));
    }

    @Test
    @DisplayName("검색 결과가 없으면(success는 true, data 빈 맵) 빈 목록을 담아 200을 응답한다")
    void noDeals_respondsEmptyList() {
        stubGroupedPricesEmpty(TRAVELPAYOUTS);

        fixedSearchRequest()
                .when()
                .get(ENDPOINT)
                .then()
                .statusCode(200)
                .body("error", nullValue())
                .body("data", hasSize(0));
    }

    @Test
    @DisplayName("Travelpayouts의 success 필드가 false면 200과 external_api_unavailable을 응답한다")
    void unsuccessfulResponse_respondsExternalApiUnavailable() {
        stubGroupedPricesUnsuccessful(TRAVELPAYOUTS);

        assertExternalApiUnavailable(fixedSearchRequest().when().get(ENDPOINT));
    }

    @Test
    @DisplayName("Travelpayouts가 서버 오류를 내면 200과 external_api_unavailable을 응답한다")
    void externalServerError_respondsExternalApiUnavailable() {
        stubGroupedPricesStatus(TRAVELPAYOUTS, 500);

        assertExternalApiUnavailable(fixedSearchRequest().when().get(ENDPOINT));
    }

    @Test
    @DisplayName("Travelpayouts 연결이 끊기면 200과 external_api_unavailable을 응답한다")
    void externalConnectionReset_respondsExternalApiUnavailable() {
        stubGroupedPricesConnectionReset(TRAVELPAYOUTS);

        assertExternalApiUnavailable(fixedSearchRequest().when().get(ENDPOINT));
    }

    @Test
    @DisplayName("origin이 없으면 400과 검증 오류 상세를 응답하고 Travelpayouts는 부르지 않는다")
    void missingOrigin_respondsValidationError() {
        given().queryParam("searchMode", "fixed")
                .queryParam("destination", DESTINATION)
                .queryParam("dateFrom", departureAt().toString())
                .queryParam("dateTo", returnAt().toString())
                .when()
                .get(ENDPOINT)
                .then()
                .statusCode(400)
                .body("error.code", equalTo("validation_failed"))
                .body("error.details[0].field", equalTo("origin"))
                .body("error.details[0].code", equalTo("required"));

        verifyGroupedPricesNotCalled(TRAVELPAYOUTS);
    }

    /**
     * 주말/공휴일 출발·경유 포함 여부는 기본값이 전부 "제외"다(FlightDealPostFilter). 상대
     * 날짜(+30일)를 쓰는 이 테스트는 실행 시점에 따라 그 요일이 달라지므로, 기본값에 맡기면
     * 실행할 때마다 결과가 달라지는 flaky 테스트가 된다 — 셋 다 명시적으로 켜서 고정한다.
     */
    private RequestSpecification fixedSearchRequest() {
        return given().queryParam("origin", ORIGIN)
                .queryParam("destination", DESTINATION)
                .queryParam("searchMode", "fixed")
                .queryParam("dateFrom", departureAt().toString())
                .queryParam("dateTo", returnAt().toString())
                .queryParam("includeWeekend", "true")
                .queryParam("includeHoliday", "true")
                .queryParam("includeTransfer", "true");
    }

    private LocalDate departureAt() {
        return LocalDate.now().plusDays(30);
    }

    private LocalDate returnAt() {
        return LocalDate.now().plusDays(35);
    }

    private void assertExternalApiUnavailable(Response response) {
        response.then()
                .statusCode(200)
                .body("data", nullValue())
                .body("error.code", equalTo("external_api_unavailable"));
    }
}
