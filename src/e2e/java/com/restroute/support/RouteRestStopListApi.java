package com.restroute.support;

import static com.restroute.support.RouteFixtures.DESTINATION_LATITUDE;
import static com.restroute.support.RouteFixtures.DESTINATION_LONGITUDE;
import static com.restroute.support.RouteFixtures.DESTINATION_QUERY;
import static com.restroute.support.RouteFixtures.ORIGIN_LATITUDE;
import static com.restroute.support.RouteFixtures.ORIGIN_LONGITUDE;
import static io.restassured.RestAssured.given;

import io.restassured.response.Response;

/**
 * {@code GET /api/route-rest-stops/list} 호출을 목적지 지정 방식별로 나눠 담는다. 프론트가
 * 실제로 두 가지 방식으로 이 API를 부르고, 서버가 타는 경로도 그에 따라 갈리기 때문이다.
 *
 * <ul>
 *   <li>{@link #getByDestinationQuery()} — 목적지 칩(부산역·대전역 등)이 쓰는 방식. 검색어만
 *       넘기므로 서버가 지오코딩을 먼저 해야 한다. 카카오 호출 2회(로컬 → 모빌리티).
 *   <li>{@link #getByDestinationCoordinates()} — 검색 후보를 골랐을 때 쓰는 방식. 프론트가
 *       {@code /api/place-search}에서 이미 좌표를 받아둔 상태라 지오코딩이 필요 없다.
 *       카카오 호출 1회(모빌리티만).
 * </ul>
 */
public final class RouteRestStopListApi {

    private static final String PATH = "/api/route-rest-stops/list";

    private RouteRestStopListApi() {}

    /** 목적지를 검색어로 넘긴다 — 서버가 지오코딩부터 한다. */
    public static Response getByDestinationQuery() {
        return given().param("originLat", ORIGIN_LATITUDE)
                .param("originLng", ORIGIN_LONGITUDE)
                .param("destinationQuery", DESTINATION_QUERY)
                .when()
                .get(PATH);
    }

    /** 목적지를 좌표로 넘긴다 — 서버가 지오코딩을 건너뛴다. */
    public static Response getByDestinationCoordinates() {
        return given().param("originLat", ORIGIN_LATITUDE)
                .param("originLng", ORIGIN_LONGITUDE)
                .param("destinationLat", DESTINATION_LATITUDE)
                .param("destinationLng", DESTINATION_LONGITUDE)
                .param("destinationName", DESTINATION_QUERY)
                .when()
                .get(PATH);
    }
}
