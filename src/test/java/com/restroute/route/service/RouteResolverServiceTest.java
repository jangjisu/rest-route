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
import com.restroute.route.service.RouteResolverService.RawRouteResult;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteResolverServiceTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5);

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

    @Test
    void query_geocodesDestinationAndBuildsRoute() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L, null), VERTEXES));

        RawRouteResult resolved = service.resolveDestinationAndRoute(37.0, 127.0, "부산", null, null, null);

        assertThat(resolved.destination().name()).isEqualTo("부산역");
        assertThat(resolved.destination().latitude()).isEqualTo(35.0);
        assertThat(resolved.routes()).hasSize(1);
        assertThat(resolved.routes().get(0).summary().distance()).isEqualTo(100L);
    }

    @Test
    void explicitCoordinates_skipGeocoding() {
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(10L, 20L, null), VERTEXES));

        RawRouteResult resolved = service.resolveDestinationAndRoute(37.0, 127.0, null, 35.0, 129.0, "부산항");

        assertThat(resolved.destination().name()).isEqualTo("부산항");
        verify(kakaoMapClient, never()).searchKeyword(anyString());
    }

    @Test
    void partialCoordinates_fallBackToQuery() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));

        RawRouteResult resolved = service.resolveDestinationAndRoute(37.0, 127.0, "부산", 35.0, null, "이름");

        assertThat(resolved.destination().name()).isEqualTo("부산역");
    }

    @Test
    void coordinatesWithoutName_defaultToDestination() {
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));

        assertThat(service.resolveDestinationAndRoute(37.0, 127.0, null, 35.0, 129.0, null)
                        .destination()
                        .name())
                .isEqualTo("목적지");
        assertThat(service.resolveDestinationAndRoute(37.0, 127.0, null, 35.0, 129.0, "  ")
                        .destination()
                        .name())
                .isEqualTo("목적지");
    }

    @Test
    void blankPlaceName_fallsBackToAddressName() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "", "부산 우동"));
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));

        RawRouteResult resolved = service.resolveDestinationAndRoute(37.0, 127.0, "부산", null, null, null);

        assertThat(resolved.destination().name()).isEqualTo("부산 우동");
    }

    @Test
    void emptySearchResult_throwsNotFound() {
        when(kakaoMapClient.searchKeyword("없는곳")).thenReturn(new KakaoLocalSearchResponse(List.of()));
        assertThatThrownBy(() -> service.resolveDestinationAndRoute(37.0, 127.0, "없는곳", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.searchKeyword("널")).thenReturn(new KakaoLocalSearchResponse(null));
        assertThatThrownBy(() -> service.resolveDestinationAndRoute(37.0, 127.0, "널", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    void unparsableDestinationCoordinates_throwNotFound() {
        when(kakaoMapClient.searchKeyword("경도없음")).thenReturn(searchResult(null, "35.0", "곳", null));
        assertThatThrownBy(() -> service.resolveDestinationAndRoute(37.0, 127.0, "경도없음", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.searchKeyword("위도없음")).thenReturn(searchResult("129.0", null, "곳", null));
        assertThatThrownBy(() -> service.resolveDestinationAndRoute(37.0, 127.0, "위도없음", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    void noSuccessfulRoute_throwsNotFound() {
        when(kakaoMapClient.searchKeyword(anyString())).thenReturn(searchResult("129.0", "35.0", "부산", null));

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(104, null, VERTEXES));
        assertThatThrownBy(() -> service.resolveDestinationAndRoute(37.0, 127.0, "부산", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(new KakaoDirectionsResponse(List.of()));
        assertThatThrownBy(() -> service.resolveDestinationAndRoute(37.0, 127.0, "부산", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(new KakaoDirectionsResponse(null));
        assertThatThrownBy(() -> service.resolveDestinationAndRoute(37.0, 127.0, "부산", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    void routeFailure_mapsMessageByResultCode() {
        when(kakaoMapClient.searchKeyword(anyString())).thenReturn(searchResult("129.0", "35.0", "부산", null));

        assertFailureMessage(105, "출발지 주변");
        assertFailureMessage(101, "출발지 주변");
        assertFailureMessage(106, "도착지 주변");
        assertFailureMessage(102, "도착지 주변");
        assertFailureMessage(104, "너무 가까워요");
        assertFailureMessage(1, "다시 확인");
    }

    private void assertFailureMessage(int resultCode, String expectedFragment) {
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(resultCode, null, VERTEXES));
        assertThatThrownBy(() -> service.resolveDestinationAndRoute(37.0, 127.0, "부산", null, null, null))
                .isInstanceOf(RouteRestStopNotFoundException.class)
                .hasMessageContaining(expectedFragment);
    }
}
