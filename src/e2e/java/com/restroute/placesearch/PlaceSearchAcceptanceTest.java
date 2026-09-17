package com.restroute.placesearch;

import static com.restroute.support.KakaoApiStubs.keywordDocument;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchConnectionReset;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchDocuments;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchNonJson;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchStatus;
import static com.restroute.support.KakaoApiStubs.stubKeywordSearchWithoutDocuments;
import static com.restroute.support.KakaoApiStubs.verifyKeywordSearchCalled;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.restroute.AcceptanceTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 목적지 검색창이 쓰는 {@code GET /api/place-search}가 카카오 장소 검색의 응답 형태에 따라
 * 사용자에게 무엇을 돌려주는지를 한자리에 모은다.
 *
 * <p>이 API의 외부 호출은 장소 검색 <b>1회</b>뿐이고, 응답은 카카오 문서를 우리 계약으로
 * 옮긴 것이다. 그래서 여기서 보는 것은 두 가지다 — 카카오 필드가 실제로 <b>역직렬화되어</b>
 * 우리 필드로 옮겨지는가({@code place_name}→{@code name}, {@code x}→{@code longitude}),
 * 그리고 <b>외부가 실패할 때 무엇이 사용자에게 나가는가</b>.
 *
 * <p>{@code src/test}의 {@code PlaceSearchServiceTest}는 Mockito로 클라이언트를 통째로
 * 바꿔치기하므로 JSON 매핑과 축(경도/위도) 자체는 실행되지 않는다. 그 구간이 이 계층의 몫이다.
 *
 * <p>실패 케이스의 기대값은 설계 의도가 아니라 <b>관찰된 현재 동작</b>이다 — 관찰 결과 전체는
 * {@code harness/runs/current/kakao-endpoint-failure-as-is.md} 참고.
 */
class PlaceSearchAcceptanceTest extends AcceptanceTest {

    private static final String PATH = "/api/place-search";

    private static final String EXTERNAL_API_UNAVAILABLE = "EXTERNAL_API_UNAVAILABLE";
    private static final int SUCCESS_STATUS = 200;
    private static final int BAD_REQUEST_STATUS = 400;

    private Response search(String query) {
        return given().param("query", query).when().get(PATH);
    }

    // --- 정상 ---------------------------------------------------------------

    /**
     * 카카오는 경도를 {@code x}, 위도를 {@code y}로 준다. 축이 바뀌면 검색 결과가 엉뚱한 곳을
     * 가리키게 되는데, 클라이언트를 목으로 바꾸는 단위 테스트로는 이 매핑이 검증되지 않는다.
     */
    @Test
    @DisplayName("카카오 검색 결과를 후보 목록으로 옮겨준다")
    void keywordSearchResults_becomeCandidates() {
        stubKeywordSearchDocuments(
                KAKAO,
                keywordDocument("부산역", "부산 동구 초량동", "129.0403", "35.1148"),
                keywordDocument("부산역환승센터", "부산 동구 초량동 1000", "129.0410", "35.1150"));

        search("부산역")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(2))
                .body("data[0].name", equalTo("부산역"))
                .body("data[0].address", equalTo("부산 동구 초량동"))
                .body("data[0].latitude", equalTo(35.1148f))
                .body("data[0].longitude", equalTo(129.0403f))
                .body("data[1].name", equalTo("부산역환승센터"));

        verifyKeywordSearchCalled(KAKAO, "부산역");
    }

    /** 카카오가 주소만 있고 장소명이 없는 문서를 주는 경우가 있다. 이름 자리를 비워 보내지 않는다. */
    @Test
    @DisplayName("장소명이 비어 있으면 주소명을 이름으로 쓴다")
    void blankPlaceName_fallsBackToAddressName() {
        stubKeywordSearchDocuments(KAKAO, keywordDocument("", "부산 동구 초량동", "129.0403", "35.1148"));

        search("부산역")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(1))
                .body("data[0].name", equalTo("부산 동구 초량동"));
    }

    /**
     * 멀쩡한 후보를 함께 두는 것이 핵심이다 — 깨진 것만 두면 전부 버려도 통과해버려
     * "그 후보만 빠진다"를 아무것도 증명하지 못한다.
     */
    @Test
    @DisplayName("좌표를 숫자로 읽을 수 없는 후보만 빠지고 나머지는 남는다")
    void unparsableCoordinates_dropOnlyThatCandidate() {
        stubKeywordSearchDocuments(
                KAKAO,
                keywordDocument("좌표없음", "주소", null, "35.1148"),
                keywordDocument("좌표깨짐", "주소", "abc", "35.1148"),
                keywordDocument("부산역", "부산 동구 초량동", "129.0403", "35.1148"));

        search("부산역").then().statusCode(SUCCESS_STATUS).body("data", hasSize(1)).body("data[0].name", equalTo("부산역"));
    }

    @Test
    @DisplayName("검색 결과가 없으면 실패가 아니라 빈 목록이 나간다")
    void noSearchResult_respondsEmptyList() {
        stubKeywordSearchDocuments(KAKAO);

        search("없는곳")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(0));
    }

    /** 빈 배열과 갈래가 다르다 — 역직렬화가 null을 견디는지는 실제로 JSON을 태워야 확인된다. */
    @Test
    @DisplayName("documents 키가 아예 없어도 빈 목록으로 견딘다")
    void missingDocumentsKey_respondsEmptyList() {
        stubKeywordSearchWithoutDocuments(KAKAO);

        search("없는곳").then().statusCode(SUCCESS_STATUS).body("data", hasSize(0));
    }

    // --- 잘못된 요청 ---------------------------------------------------------

    @Test
    @DisplayName("query 파라미터가 없으면 400이 나간다")
    void missingQueryParameter_responds400() {
        given().when().get(PATH).then().statusCode(BAD_REQUEST_STATUS).body("code", equalTo("INVALID_PARAMETER"));
    }

    /**
     * 빈 검색어는 걸러지지 않는다 — 파라미터가 <b>있으므로</b> 스프링 바인딩을 통과하고,
     * 서비스도 검사하지 않아 그대로 카카오로 나간다. 사용자 입력 검증이 외부 호출 뒤로 밀려
     * 쿼터와 지연을 먼저 쓰는 구조이며, 여기서는 그 사실을 고정만 해둔다.
     */
    @Test
    @DisplayName("빈 검색어도 걸러지지 않고 카카오까지 나간다")
    void blankQuery_reachesExternalApi() {
        stubKeywordSearchDocuments(KAKAO, keywordDocument("부산역", "부산 동구 초량동", "129.0403", "35.1148"));

        search("").then().statusCode(SUCCESS_STATUS).body("code", equalTo("SUCCESS"));

        verifyKeywordSearchCalled(KAKAO, "");
    }

    // --- 외부 API 실패: 원인과 무관하게 200 + EXTERNAL_API_UNAVAILABLE ---------

    @Test
    @DisplayName("장소 검색이 HTTP 500이면 200 + EXTERNAL_API_UNAVAILABLE로 나간다")
    void keywordSearchServerError_respondsExternalApiUnavailable() {
        stubKeywordSearchStatus(KAKAO, 500);

        search("부산역")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo(EXTERNAL_API_UNAVAILABLE))
                .body("data", nullValue());
    }

    /** 429는 백오프가, 500은 재시도가 맞는 대응인데 지금은 갈리지 않는다는 것을 고정한다. */
    @Test
    @DisplayName("장소 검색이 HTTP 429여도 서버 장애와 똑같이 처리된다")
    void keywordSearchQuotaExceeded_isNotDistinguishedFromServerError() {
        stubKeywordSearchStatus(KAKAO, 429);

        search("부산역").then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    @Test
    @DisplayName("에러 응답 본문이 JSON이 아니어도 파싱 실패로 새지 않는다")
    void keywordSearchNonJsonErrorBody_respondsExternalApiUnavailable() {
        stubKeywordSearchNonJson(KAKAO, 503);

        search("부산역").then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }

    /** 상태코드조차 못 받는 갈래 — 서버가 내려갔거나 중간 네트워크가 끊긴 상황. */
    @Test
    @DisplayName("연결이 끊겨도 상태코드 실패와 같은 응답이 된다")
    void keywordSearchConnectionReset_respondsExternalApiUnavailable() {
        stubKeywordSearchConnectionReset(KAKAO);

        search("부산역").then().statusCode(SUCCESS_STATUS).body("code", equalTo(EXTERNAL_API_UNAVAILABLE));
    }
}
