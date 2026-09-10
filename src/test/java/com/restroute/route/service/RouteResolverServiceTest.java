package com.restroute.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.common.client.KakaoMapClient;
import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.common.client.response.KakaoDirectionsResponse.Road;
import com.restroute.common.client.response.KakaoDirectionsResponse.Route;
import com.restroute.common.client.response.KakaoDirectionsResponse.Section;
import com.restroute.common.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.common.client.response.KakaoLocalSearchResponse;
import com.restroute.common.client.response.KakaoLocalSearchResponse.Document;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.service.RouteResolverService.RawRouteResult;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteResolverServiceTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5);
    private static final Destination BUSAN = Destination.of("부산역", 35.0, 129.0);

    @Mock
    private KakaoMapClient kakaoMapClient;

    private RouteResolverService service;

    @BeforeEach
    void setUp() {
        service = new RouteResolverService(kakaoMapClient);
    }

    private KakaoLocalSearchResponse searchResult(String x, String y, String placeName, String addressName) {
        return new KakaoLocalSearchResponse(List.of(new Document(x, y, placeName, addressName)));
    }

    private KakaoDirectionsResponse directions(int code, Summary summary, List<Double> vertexes) {
        Route route = new Route(code, summary, List.of(new Section(List.of(new Road(vertexes)))));
        return new KakaoDirectionsResponse(List.of(route));
    }

    // --- 목적지 해석 ----------------------------------------------------------

    @Test
    @DisplayName("검색어는 장소 검색으로 좌표를 얻는다")
    void query_geocodesDestination() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));

        Destination destination = service.resolveDestination("부산", null, null, null);

        assertThat(destination.name()).isEqualTo("부산역");
        assertThat(destination.latitude()).isEqualTo(35.0);
        assertThat(destination.longitude()).isEqualTo(129.0);
    }

    @Test
    @DisplayName("좌표가 함께 오면 장소 검색을 부르지 않는다")
    void explicitCoordinates_skipPlaceSearch() {
        Destination destination = service.resolveDestination(null, 35.0, 129.0, "부산항");

        assertThat(destination.name()).isEqualTo("부산항");
        verify(kakaoMapClient, never()).searchKeyword(anyString());
    }

    @Test
    @DisplayName("좌표가 한쪽만 오면 검색어로 되돌아간다")
    void partialCoordinates_fallBackToQuery() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));

        Destination destination = service.resolveDestination("부산", 35.0, null, "이름");

        assertThat(destination.name()).isEqualTo("부산역");
    }

    @Test
    @DisplayName("좌표만 있고 이름이 없으면 기본 표시명을 쓴다")
    void coordinatesWithoutName_defaultToDestination() {
        assertThat(service.resolveDestination(null, 35.0, 129.0, null).name()).isEqualTo("목적지");
        assertThat(service.resolveDestination(null, 35.0, 129.0, "  ").name()).isEqualTo("목적지");
    }

    @Test
    @DisplayName("장소명이 비면 주소명으로 대체한다")
    void blankPlaceName_fallsBackToAddressName() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "", "부산 우동"));

        assertThat(service.resolveDestination("부산", null, null, null).name()).isEqualTo("부산 우동");
    }

    @Test
    @DisplayName("검색 결과가 없으면 NotFound다")
    void emptySearchResult_throwsNotFound() {
        when(kakaoMapClient.searchKeyword("없는곳")).thenReturn(new KakaoLocalSearchResponse(List.of()));
        assertThatThrownBy(() -> service.resolveDestination("없는곳", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.searchKeyword("널")).thenReturn(new KakaoLocalSearchResponse(null));
        assertThatThrownBy(() -> service.resolveDestination("널", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("검색 결과의 좌표를 숫자로 읽을 수 없으면 NotFound다")
    void unparsableDestinationCoordinates_throwNotFound() {
        when(kakaoMapClient.searchKeyword("경도없음")).thenReturn(searchResult(null, "35.0", "곳", null));
        assertThatThrownBy(() -> service.resolveDestination("경도없음", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.searchKeyword("위도없음")).thenReturn(searchResult("129.0", null, "곳", null));
        assertThatThrownBy(() -> service.resolveDestination("위도없음", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    // --- 길찾기 --------------------------------------------------------------

    @Test
    @DisplayName("정해진 목적지로 원본 경로를 받아온다")
    void resolveRoute_returnsRawRoutes() {
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L, null), VERTEXES));

        RawRouteResult resolved = service.resolveRoute(37.0, 127.0, BUSAN);

        assertThat(resolved.destination()).isEqualTo(BUSAN);
        assertThat(resolved.routes()).hasSize(1);
        assertThat(resolved.routes().get(0).summary().distance()).isEqualTo(100L);
    }

    @Test
    @DisplayName("성공한 경로가 하나도 없으면 NotFound다")
    void noSuccessfulRoute_throwsNotFound() {
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(104, null, VERTEXES));
        assertThatThrownBy(() -> service.resolveRoute(37.0, 127.0, BUSAN))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(new KakaoDirectionsResponse(List.of()));
        assertThatThrownBy(() -> service.resolveRoute(37.0, 127.0, BUSAN))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(new KakaoDirectionsResponse(null));
        assertThatThrownBy(() -> service.resolveRoute(37.0, 127.0, BUSAN))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("결과 코드별로 안내 문구가 갈린다")
    void routeFailure_mapsMessageByResultCode() {
        assertFailureMessage(105, "출발지 주변");
        assertFailureMessage(101, "출발지 주변");
        assertFailureMessage(106, "도착지 주변");
        assertFailureMessage(102, "도착지 주변");
        assertFailureMessage(104, "너무 가까워요");
        assertFailureMessage(1, "다시 확인");
    }

    private void assertFailureMessage(int resultCode, String expectedFragment) {
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(resultCode, null, VERTEXES));
        assertThatThrownBy(() -> service.resolveRoute(37.0, 127.0, BUSAN))
                .isInstanceOf(RouteRestStopNotFoundException.class)
                .hasMessageContaining(expectedFragment);
    }
}
