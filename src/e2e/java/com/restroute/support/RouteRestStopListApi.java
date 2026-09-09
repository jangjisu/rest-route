package com.restroute.support;

import static com.restroute.support.RouteFixtures.DESTINATION_LATITUDE;
import static com.restroute.support.RouteFixtures.DESTINATION_LONGITUDE;
import static com.restroute.support.RouteFixtures.KNOWN_DESTINATION_NAME;
import static com.restroute.support.RouteFixtures.ORIGIN_LATITUDE;
import static com.restroute.support.RouteFixtures.ORIGIN_LONGITUDE;
import static io.restassured.RestAssured.given;

import io.restassured.response.Response;

/**
 * {@code GET /api/route-rest-stops/list} 호출을 목적지 지정 방식별로 나눠 담는다. 프론트가
 * 두 가지 방식으로 이 API를 부르지만, <b>어느 쪽도 지오코딩을 타지 않는다</b> — 이름으로 넘기면
 * 서버가 알고 있는 목적지 목록에서 좌표를 찾고, 좌표로 넘기면 그대로 쓴다.
 *
 * <ul>
 *   <li>{@link #getByDestinationName()} — 목적지 칩이 쓰는 방식. 서버가 이름으로 좌표를 안다.
 *   <li>{@link #getByDestinationCoordinates()} — 검색 후보를 골랐을 때 쓰는 방식. 프론트가
 *       {@code /api/place-search}에서 이미 좌표를 받아둔 상태다.
 * </ul>
 */
public final class RouteRestStopListApi {

    private static final String PATH = "/api/route-rest-stops/list";

    private RouteRestStopListApi() {}

    /** 서버가 좌표를 알고 있는 목적지를 이름으로 넘긴다. */
    public static Response getByDestinationName() {
        return getByDestinationName(KNOWN_DESTINATION_NAME);
    }

    public static Response getByDestinationName(String destinationName) {
        return given().param("originLat", ORIGIN_LATITUDE)
                .param("originLng", ORIGIN_LONGITUDE)
                .param("destinationName", destinationName)
                .when()
                .get(PATH);
    }

    /** 목적지를 좌표로 넘긴다. */
    public static Response getByDestinationCoordinates() {
        return given().param("originLat", ORIGIN_LATITUDE)
                .param("originLng", ORIGIN_LONGITUDE)
                .param("destinationLat", DESTINATION_LATITUDE)
                .param("destinationLng", DESTINATION_LONGITUDE)
                .param("destinationName", KNOWN_DESTINATION_NAME)
                .when()
                .get(PATH);
    }
}
