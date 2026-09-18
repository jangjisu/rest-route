package com.restroute.oilprice;

import static com.restroute.support.OpinetApiStubs.stubAverageOilPrices;
import static com.restroute.support.OpinetApiStubs.stubAverageOilPricesConnectionReset;
import static com.restroute.support.OpinetApiStubs.stubAverageOilPricesMissingResult;
import static com.restroute.support.OpinetApiStubs.stubAverageOilPricesStatus;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code GET /api/national-oil-prices/summary}.
 *
 * <p>이 계층에서 볼 값은 하나뿐이다 — {@link com.restroute.oilprice.service.NationalOilPriceService}가
 * 오늘자 캐시가 없으면 <b>요청을 처리하는 도중에</b> 오피넷을 호출한다(배치 스케줄러가 아니다).
 * 그래서 다른 조회 전용 엔드포인트와 달리 외부 API 실패 갈래가 있다.
 *
 * <p>기대값은 실제 코드({@code NationalOilPriceController}/{@code NationalOilPriceService})를
 * 읽고 확정한 것이다 — 오피넷 실패는 카카오·ExApi와 같은 house convention대로 <b>HTTP 200 +
 * {@code EXTERNAL_API_UNAVAILABLE}</b>로 나간다({@code ResponseCode.EXTERNAL_API_UNAVAILABLE}의
 * HTTP 상태 자체가 200이다).
 */
class NationalOilPriceAcceptanceTest extends AcceptanceTest {

    private static final String ENDPOINT = "/api/national-oil-prices/summary";
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private static final String EXTERNAL_API_UNAVAILABLE = "EXTERNAL_API_UNAVAILABLE";

    @Test
    @DisplayName("오피넷 조회에 성공하면 오늘 날짜와 유종별 평균가를 담아 200을 응답한다")
    void success_respondsTodaySummary() {
        // TRADE_DT는 DateTimeFormatter.BASIC_ISO_DATE(yyyyMMdd, 대시 없음)로 파싱된다
        // (NationalOilPriceEntity.from()).
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        stubAverageOilPrices(OPINET, today, "1700.00", "1550.00", "900.00");

        given().when()
                .get(ENDPOINT)
                .then()
                .statusCode(200)
                .body("code", equalTo("SUCCESS"))
                .body("data.tradeDate", equalTo(LocalDate.now().format(DISPLAY_DATE_FORMAT)))
                // AverageOilPrice.price는 NationalOilPriceEntity.formattedPrice()가 만든
                // 표시용 문자열이다 — 오피넷 원본 "1700.00"이 "1,700원"으로 바뀐다.
                .body("data.gasoline.price", equalTo("1,700원"))
                .body("data.diesel.price", equalTo("1,550원"))
                .body("data.lpg.price", equalTo("900원"));
    }

    @Test
    @DisplayName("오피넷 응답에 RESULT.OIL이 없으면 200과 EXTERNAL_API_UNAVAILABLE을 응답한다")
    void missingResult_respondsExternalApiUnavailable() {
        stubAverageOilPricesMissingResult(OPINET);

        assertExternalApiUnavailable(given().when().get(ENDPOINT));
    }

    @Test
    @DisplayName("오피넷이 서버 오류를 내면 200과 EXTERNAL_API_UNAVAILABLE을 응답한다")
    void externalServerError_respondsExternalApiUnavailable() {
        stubAverageOilPricesStatus(OPINET, 500);

        assertExternalApiUnavailable(given().when().get(ENDPOINT));
    }

    @Test
    @DisplayName("오피넷 연결이 끊기면 200과 EXTERNAL_API_UNAVAILABLE을 응답한다")
    void externalConnectionReset_respondsExternalApiUnavailable() {
        stubAverageOilPricesConnectionReset(OPINET);

        assertExternalApiUnavailable(given().when().get(ENDPOINT));
    }

    private void assertExternalApiUnavailable(Response response) {
        response.then()
                .statusCode(200)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE))
                .body("data", nullValue());
    }
}
