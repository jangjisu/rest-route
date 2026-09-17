package com.restroute.support;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 브라우저가 CSRF 토큰을 얻는 경로를 그대로 재현한다 — 페이지를 한 번 받아 {@code <meta>}에 실린
 * 토큰과 헤더명을 읽고, 같은 세션으로 요청을 보낸다.
 *
 * <p>{@code SecurityConfig}가 {@code /api/**}를 열어두면서도 CSRF는 끄지 않았기 때문에, 변경
 * 요청은 이 토큰이 없으면 403이다. 프론트({@code rest-stop-detail-request.js})가 같은 meta를
 * 읽어 헤더에 싣는다.
 */
public final class CsrfTokens {

    /** 토큰을 실어 내려주는 공개 페이지. 프론트도 이 페이지에서 값을 읽는다. */
    private static final String TOKEN_BEARING_PAGE = "/";

    private static final Pattern TOKEN = metaContent("_csrf");
    private static final Pattern HEADER_NAME = metaContent("_csrf_header");

    private CsrfTokens() {}

    private static Pattern metaContent(String metaName) {
        return Pattern.compile("<meta name=\"" + metaName + "\" content=\"([^\"]*)\"");
    }

    /**
     * 토큰과 세션 쿠키를 얹은 요청 사양을 돌려준다. 둘 중 하나만 있으면 통하지 않으므로 함께 싣는다.
     */
    public static RequestSpecification authorizedRequest() {
        Response page = given().when().get(TOKEN_BEARING_PAGE);
        String token = extract(TOKEN, page.asString(), "_csrf");
        String headerName = extract(HEADER_NAME, page.asString(), "_csrf_header");

        return given().header(headerName, token).cookies(page.getCookies());
    }

    private static String extract(Pattern pattern, String html, String metaName) {
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find() || matcher.group(1).isBlank()) {
            throw new IllegalStateException(
                    TOKEN_BEARING_PAGE + " 페이지에서 " + metaName + " meta를 읽지 못했다. 페이지가 토큰을 더는 싣지 않는지 확인이 필요하다.");
        }
        return matcher.group(1);
    }
}
