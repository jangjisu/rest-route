package com.restroute.support;

import static io.restassured.RestAssured.given;

import com.restroute.admin.domain.AdminRole;
import com.restroute.admin.domain.AdminUserEntity;
import com.restroute.admin.repository.AdminUserRepository;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 관리자 화면이 실제로 밟는 인증 흐름을 재현한다 — 계정을 심고, 폼 로그인을 하고, 로그인 뒤
 * 새로 발급된 세션과 CSRF 토큰으로 요청한다.
 *
 * <p>로그인 시점에 <b>둘 다 회전한다</b>는 점이 핵심이다. 스프링 시큐리티는 세션 고정 공격을
 * 막으려고 인증 성공 시 세션을 새로 만들고, CSRF 토큰도 그 세션에 맞춰 다시 발급한다. 로그인
 * 전에 받아둔 값을 그대로 쓰면 403이 되므로 로그인 <b>후에</b> 다시 읽어야 한다.
 */
public final class AdminSessions {

    public static final String USERNAME = "e2e-admin";
    public static final String PASSWORD = "e2e-password";

    /** 토큰과 세션을 함께 실어 내려주는 공개 페이지. */
    private static final String TOKEN_BEARING_PAGE = "/";

    private static final String LOGIN_PATH = "/login";
    private static final int FOUND_STATUS = 302;

    private static final Pattern TOKEN = metaContent("_csrf");
    private static final Pattern HEADER_NAME = metaContent("_csrf_header");

    private AdminSessions() {}

    private static Pattern metaContent(String metaName) {
        return Pattern.compile("<meta name=\"" + metaName + "\" content=\"([^\"]*)\"");
    }

    /** 관리자 계정을 심는다. 비밀번호는 로그인 때 대조되므로 실제 인코더로 암호화해 저장한다. */
    public static void saveAdminUser(AdminUserRepository repository, PasswordEncoder passwordEncoder) {
        repository.save(AdminUserEntity.of(USERNAME, passwordEncoder.encode(PASSWORD), AdminRole.ADMIN));
    }

    /**
     * 로그인까지 마친 요청 사양을 돌려준다. 관리자 계정이 미리 심겨 있어야 한다
     * ({@link #saveAdminUser}).
     */
    public static RequestSpecification loggedIn() {
        Response anonymousPage = given().when().get(TOKEN_BEARING_PAGE);
        Map<String, String> session = new HashMap<>(anonymousPage.getCookies());

        Response login = given().cookies(session)
                .formParams(Map.of(
                        "username", USERNAME,
                        "password", PASSWORD,
                        "_csrf", extract(TOKEN, anonymousPage.asString(), "_csrf")))
                .redirects()
                .follow(false)
                .when()
                .post(LOGIN_PATH);

        if (login.statusCode() != FOUND_STATUS
                || String.valueOf(login.header("Location")).contains("error")) {
            throw new IllegalStateException(
                    "관리자 로그인에 실패했다. status=" + login.statusCode() + ", location=" + login.header("Location"));
        }

        // 쿠키를 통째로 갈아끼우지 않고 누적한다 — 로그인 이후의 페이지가 Set-Cookie를 내려주지
        // 않을 수 있어서, 응답 쿠키로 덮어쓰면 인증된 세션을 잃고 조용히 익명 요청이 된다.
        session.putAll(login.getCookies());
        Response authenticatedPage = given().cookies(session).when().get(TOKEN_BEARING_PAGE);
        session.putAll(authenticatedPage.getCookies());

        String html = authenticatedPage.asString();
        return given().cookies(session)
                .header(extract(HEADER_NAME, html, "_csrf_header"), extract(TOKEN, html, "_csrf"));
    }

    private static String extract(Pattern pattern, String html, String metaName) {
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find() || matcher.group(1).isBlank()) {
            throw new IllegalStateException(TOKEN_BEARING_PAGE + " 페이지에서 " + metaName + " meta를 읽지 못했다.");
        }
        return matcher.group(1);
    }
}
