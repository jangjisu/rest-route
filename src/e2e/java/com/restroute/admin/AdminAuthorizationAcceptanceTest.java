package com.restroute.admin;

import static com.restroute.support.AdminSessions.loggedIn;
import static com.restroute.support.AdminSessions.saveAdminUser;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.restroute.AcceptanceTest;
import com.restroute.admin.repository.AdminUserRepository;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 관리자 영역 39개 엔드포인트가 <b>빠짐없이</b> 막혀 있는지 확인한다.
 *
 * <p>{@code SecurityConfigTest}가 이미 인가를 보지만 대표 경로 6개만 확인한다 — 규칙이
 * {@code /admin/**}, {@code /api/admin/**} 패턴이라 그것으로 충분하다는 판단이었다. 다만
 * <b>새 관리자 엔드포인트가 그 패턴 밖에 생기면</b> 아무도 알려주지 않는다. 여기서는 실제
 * 컨트롤러에 선언된 경로를 모두 나열해, 패턴을 벗어난 것이 생기면 빨간불이 되게 한다.
 *
 * <p>관찰에서 드러난 두 갈래를 나눠서 고정한다.
 *
 * <ul>
 *   <li><b>조회</b>는 302로 로그인 페이지에 보낸다 — API도 JSON 401이 아니라 리다이렉트다.
 *   <li><b>변경</b>은 CSRF 토큰이 없으면 403이다. 인증 여부를 보기 <b>전에</b> CSRF 필터가
 *       먼저 자르기 때문이고, 토큰을 실으면 그때 비로소 302가 된다.
 * </ul>
 *
 * <p>기대값은 관찰된 현재 동작이다 — {@code harness/runs/current/admin-endpoint-as-is.md} 참고.
 */
class AdminAuthorizationAcceptanceTest extends AcceptanceTest {

    private static final int SUCCESS_STATUS = 200;
    private static final int FOUND_STATUS = 302;
    private static final int FORBIDDEN_STATUS = 403;

    private static final String LOGIN_PATH = "/login";

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RequestSpecification withoutFollowingRedirects() {
        return given().redirects().follow(false);
    }

    private Response send(RequestSpecification specification, String verb, String path) {
        return switch (verb) {
            case "GET" -> specification.when().get(path);
            case "POST" -> specification.when().post(path);
            case "PUT" -> specification.when().put(path);
            case "DELETE" -> specification.when().delete(path);
            default -> throw new IllegalArgumentException("모르는 메서드: " + verb);
        };
    }

    // --- 조회: 로그인 페이지로 보낸다 -------------------------------------------

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(
            strings = {
                "/admin",
                "/admin/flights/holidays",
                "/admin/rest-stops/edit",
                "/admin/rest-stops/foods",
                "/admin/rest-stops/images",
                "/admin/rest-stops/oil-links",
                "/admin/rest-stops/restroom-links",
                "/api/admin/dashboard",
                "/api/admin/flights/holidays",
                "/api/admin/oil-stations/search",
                "/api/admin/rest-stop-restrooms/search",
                "/api/admin/rest-stops/oil-links",
                "/api/admin/rest-stops/restroom-links",
                "/api/admin/rest-stops/A00001/editable",
                "/api/admin/rest-stops/A00001/foods",
                "/api/admin/rest-stops/A00001/foods/1/image"
            })
    @DisplayName("로그인하지 않은 관리자 조회는 로그인 페이지로 보낸다")
    void unauthenticatedGet_redirectsToLogin(String path) {
        Response response = withoutFollowingRedirects().when().get(path);

        assertThat(response.statusCode()).isEqualTo(FOUND_STATUS);
        assertThat(response.header("Location")).endsWith(LOGIN_PATH);
    }

    // --- 변경: CSRF가 인증보다 먼저 자른다 --------------------------------------

    /**
     * 토큰이 없으면 인증 여부를 따지기도 전에 막힌다. 본문이 {@code ApiResponse}가 아니라 스프링
     * 기본 오류 형식인 것이 필터 단계에서 잘렸다는 표시다.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "POST,   /api/admin/flights/holidays",
        "DELETE, /api/admin/flights/holidays/1",
        "PUT,    /api/admin/oil-stations/1/link",
        "DELETE, /api/admin/oil-stations/1/link",
        "DELETE, /api/admin/oil-stations/1/override",
        "PUT,    /api/admin/rest-stop-restrooms/1/link",
        "DELETE, /api/admin/rest-stop-restrooms/1/link",
        "POST,   /api/admin/rest-stops",
        "POST,   /api/admin/rest-stops/restrooms",
        "POST,   /api/admin/rest-stops/usage-snapshots",
        "PUT,    /api/admin/rest-stops/A00001/editable",
        "DELETE, /api/admin/rest-stops/A00001/editable/override",
        "POST,   /api/admin/rest-stops/A00001/foods",
        "PUT,    /api/admin/rest-stops/A00001/foods/1",
        "DELETE, /api/admin/rest-stops/A00001/foods/1",
        "DELETE, /api/admin/rest-stops/A00001/foods/1/override",
        "PUT,    /api/admin/rest-stops/A00001/foods/1/image",
        "DELETE, /api/admin/rest-stops/A00001/foods/1/image",
        "PUT,    /api/admin/rest-stops/A00001/image",
        "DELETE, /api/admin/rest-stops/A00001/image",
        "POST,   /api/admin/sales-rankings/backfill",
        "POST,   /api/admin/sales-rankings/products",
        "POST,   /api/admin/sales-rankings/stores"
    })
    @DisplayName("로그인하지 않은 관리자 변경은 CSRF 단계에서 403으로 막힌다")
    void unauthenticatedMutation_isBlockedByCsrf(String verb, String path) {
        send(withoutFollowingRedirects(), verb.trim(), path.trim())
                .then()
                .statusCode(FORBIDDEN_STATUS)
                .body("error", equalTo("Forbidden"));
    }

    /**
     * 토큰을 제대로 실으면 CSRF 단계는 통과하고, 그제서야 인증이 없다는 이유로 로그인 페이지로
     * 간다. 위 테스트의 403이 "인가가 걸렸다"가 아니라 "CSRF가 먼저 걸렸다"임을 보이는 대조군이라
     * 둘이 짝으로 있어야 의미가 있다.
     */
    @Test
    @DisplayName("CSRF 토큰만 실으면 403을 지나 로그인 페이지로 간다")
    void unauthenticatedMutationWithCsrf_redirectsToLogin() {
        Response page = given().when().get("/");
        String token = page.asString().replaceAll("(?s).*<meta name=\"_csrf\" content=\"([^\"]*)\".*", "$1");
        String headerName =
                page.asString().replaceAll("(?s).*<meta name=\"_csrf_header\" content=\"([^\"]*)\".*", "$1");

        Response response = given().cookies(page.getCookies())
                .header(headerName, token)
                .redirects()
                .follow(false)
                .when()
                .post("/api/admin/sales-rankings/backfill");

        assertThat(response.statusCode()).isEqualTo(FOUND_STATUS);
        assertThat(response.header("Location")).endsWith(LOGIN_PATH);
    }

    // --- 로그인하면 열린다 -----------------------------------------------------

    /** 위의 차단이 "그냥 다 막혀 있다"가 아니라 인증으로 열린다는 것을 보이는 대조군이다. */
    @ParameterizedTest(name = "GET {0}")
    @ValueSource(
            strings = {
                "/admin",
                "/admin/flights/holidays",
                "/admin/rest-stops/edit",
                "/admin/rest-stops/foods",
                "/admin/rest-stops/images",
                "/admin/rest-stops/oil-links",
                "/admin/rest-stops/restroom-links"
            })
    @DisplayName("로그인하면 관리자 화면이 열린다")
    void authenticatedAdminPages_areServed(String path) {
        saveAdminUser(adminUserRepository, passwordEncoder);

        loggedIn().when().get(path).then().statusCode(SUCCESS_STATUS).contentType("text/html;charset=UTF-8");
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(
            strings = {
                "/api/admin/dashboard",
                "/api/admin/flights/holidays",
                "/api/admin/rest-stops/oil-links",
                "/api/admin/rest-stops/restroom-links"
            })
    @DisplayName("로그인하면 관리자 조회 API가 JSON을 돌려준다")
    void authenticatedAdminApis_respondJson(String path) {
        saveAdminUser(adminUserRepository, passwordEncoder);

        loggedIn().when().get(path).then().statusCode(SUCCESS_STATUS).body("code", equalTo("SUCCESS"));
    }

    /** 잘못된 비밀번호로는 세션이 열리지 않는다 — 인증이 실제로 대조되고 있다는 증거. */
    @Test
    @DisplayName("비밀번호가 틀리면 로그인에 실패한다")
    void wrongPassword_failsLogin() {
        saveAdminUser(adminUserRepository, passwordEncoder);

        Response page = given().when().get("/");
        String token = page.asString().replaceAll("(?s).*<meta name=\"_csrf\" content=\"([^\"]*)\".*", "$1");

        Response login = given().cookies(page.getCookies())
                .formParam("username", "e2e-admin")
                .formParam("password", "wrong-password")
                .formParam("_csrf", token)
                .redirects()
                .follow(false)
                .when()
                .post(LOGIN_PATH);

        assertThat(login.statusCode()).isEqualTo(FOUND_STATUS);
        assertThat(login.header("Location")).contains("error");
    }
}
