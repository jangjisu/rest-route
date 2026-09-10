package com.restroute.reststop;

import static com.restroute.support.RestStopFixtures.DETAIL_IMAGE_BYTES;
import static com.restroute.support.RestStopFixtures.LIST_IMAGE_BYTES;
import static com.restroute.support.RestStopFixtures.saveImages;
import static com.restroute.support.RestStopFixtures.saveRestStop;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.restroute.AcceptanceTest;
import com.restroute.reststop.repository.RestStopImageRepository;
import com.restroute.reststop.repository.RestStopRepository;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 휴게소 이미지 2종({@code /images/detail}, {@code /images/list})의 HTTP 캐시 계약을 고정한다.
 *
 * <p>이 파일이 이번 작업에서 <b>단위 테스트로 대체 불가능한 정도가 가장 큰</b> 곳이다. ETag 계산,
 * {@code If-None-Match} 조건부 GET, 304의 본문 없음은 실제 HTTP 왕복이 있어야만 돌고, 서비스만
 * 호출하는 테스트로는 한 줄도 검증되지 않는다.
 *
 * <p>이미지는 목록 화면에서 휴게소 수만큼 동시에 요청되므로, 304가 깨져 매번 본문을 다시 내려주면
 * 조용히 트래픽만 몇 배가 된다 — 실패해도 화면은 멀쩡해 보여서 눈으로는 알아채기 어렵다.
 *
 * <p>기대값은 관찰된 현재 동작이다 — {@code harness/runs/current/rest-stop-endpoint-as-is.md} 참고.
 */
class RestStopImageAcceptanceTest extends AcceptanceTest {

    private static final String BASE = "/api/rest-stops";
    private static final String DETAIL_SECTION = "/images/detail";
    private static final String LIST_SECTION = "/images/list";

    private static final int SUCCESS_STATUS = 200;
    private static final int NO_CONTENT_STATUS = 204;
    private static final int NOT_MODIFIED_STATUS = 304;
    private static final int NOT_FOUND_STATUS = 404;

    private static final String WEBP_CONTENT_TYPE = "image/webp";
    private static final String EXPECTED_CACHE_CONTROL = "public, no-cache";

    @Autowired
    private RestStopRepository restStopRepository;

    @Autowired
    private RestStopImageRepository restStopImageRepository;

    private Response get(String code, String section) {
        return given().when().get(BASE + "/" + code + section);
    }

    private Response getWithIfNoneMatch(String code, String section, String ifNoneMatch) {
        return given().header("If-None-Match", ifNoneMatch).when().get(BASE + "/" + code + section);
    }

    private String savedRestStopWithImages() {
        String code = saveRestStop(restStopRepository, "이미지휴게소");
        saveImages(restStopImageRepository, code);
        return code;
    }

    // --- 이미지가 없는 두 갈래 -------------------------------------------------

    /** 휴게소 자체가 없는 경우와 아래의 "휴게소는 있고 이미지가 없는" 경우는 응답이 다르다. */
    @Test
    @DisplayName("휴게소가 없으면 404가 나간다")
    void unknownRestStop_responds404() {
        get("UNKNOWN", DETAIL_SECTION).then().statusCode(NOT_FOUND_STATUS).body("code", equalTo("NOT_FOUND"));

        get("UNKNOWN", LIST_SECTION).then().statusCode(NOT_FOUND_STATUS);
    }

    /** 이미지 동기화가 아직 안 된 휴게소. 오류가 아니라 "보여줄 것이 없음"이다. */
    @Test
    @DisplayName("휴게소는 있고 이미지가 없으면 204가 나간다")
    void restStopWithoutImage_responds204() {
        String code = saveRestStop(restStopRepository, "이미지없는휴게소");

        get(code, DETAIL_SECTION).then().statusCode(NO_CONTENT_STATUS);
        get(code, LIST_SECTION).then().statusCode(NO_CONTENT_STATUS);
    }

    // --- 최초 응답 -----------------------------------------------------------

    @Test
    @DisplayName("이미지가 있으면 webp 바이너리를 ETag·캐시 헤더와 함께 돌려준다")
    void imageExists_respondsWebpWithCacheHeaders() {
        String code = savedRestStopWithImages();

        Response response = get(code, DETAIL_SECTION);

        response.then()
                .statusCode(SUCCESS_STATUS)
                .contentType(WEBP_CONTENT_TYPE)
                .header("Cache-Control", equalTo(EXPECTED_CACHE_CONTROL));
        assertThat(response.asByteArray()).isEqualTo(DETAIL_IMAGE_BYTES);
        assertThat(response.header("ETag")).isNotBlank();
    }

    /**
     * 목록용이 상세용 이미지를 받으면 목록 화면이 큰 이미지를 휴게소 수만큼 내려받게 된다.
     * 둘이 <b>서로 다르다</b>는 것이 계약이다.
     */
    @Test
    @DisplayName("상세용과 목록용은 서로 다른 바이트와 다른 ETag를 준다")
    void detailAndListImages_differ() {
        String code = savedRestStopWithImages();

        Response detail = get(code, DETAIL_SECTION);
        Response list = get(code, LIST_SECTION);

        assertThat(detail.asByteArray()).isEqualTo(DETAIL_IMAGE_BYTES);
        assertThat(list.asByteArray()).isEqualTo(LIST_IMAGE_BYTES);
        assertThat(detail.header("ETag")).isNotEqualTo(list.header("ETag"));
    }

    // --- 조건부 GET ----------------------------------------------------------

    @Test
    @DisplayName("같은 ETag를 다시 보내면 304와 함께 본문 없이 돌아온다")
    void sameETag_respondsNotModifiedWithoutBody() {
        String code = savedRestStopWithImages();
        String eTag = get(code, DETAIL_SECTION).header("ETag");

        Response cached = getWithIfNoneMatch(code, DETAIL_SECTION, eTag);

        cached.then()
                .statusCode(NOT_MODIFIED_STATUS)
                .header("ETag", equalTo(eTag))
                .header("Cache-Control", equalTo(EXPECTED_CACHE_CONTROL));
        assertThat(cached.asByteArray()).isEmpty();
    }

    /** 컨트롤러가 {@code "*"}를 따로 분기하고 있어 같은 결과라도 갈래가 다르다. */
    @Test
    @DisplayName("If-None-Match가 *여도 304다")
    void wildcardIfNoneMatch_respondsNotModified() {
        String code = savedRestStopWithImages();

        getWithIfNoneMatch(code, DETAIL_SECTION, "*").then().statusCode(NOT_MODIFIED_STATUS);
    }

    /** 캐시가 낡았을 때 새 본문을 받는 경로. 이게 없으면 304 테스트가 아무것도 증명하지 못한다. */
    @Test
    @DisplayName("다른 ETag를 보내면 본문을 새로 내려준다")
    void staleETag_respondsFullBody() {
        String code = savedRestStopWithImages();

        Response response = getWithIfNoneMatch(code, DETAIL_SECTION, "\"stale-etag\"");

        response.then().statusCode(SUCCESS_STATUS).contentType(WEBP_CONTENT_TYPE);
        assertThat(response.asByteArray()).isEqualTo(DETAIL_IMAGE_BYTES);
    }

    @Test
    @DisplayName("ETag는 이미지 내용에서 나오므로 다시 요청해도 같은 값이다")
    void eTag_isStableAcrossRequests() {
        String code = savedRestStopWithImages();

        assertThat(get(code, DETAIL_SECTION).header("ETag"))
                .isEqualTo(get(code, DETAIL_SECTION).header("ETag"));
    }
}
