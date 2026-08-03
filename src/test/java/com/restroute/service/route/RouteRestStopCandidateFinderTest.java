package com.restroute.service.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.client.KakaoMapClient;
import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.client.response.KakaoDirectionsResponse.Road;
import com.restroute.client.response.KakaoDirectionsResponse.Route;
import com.restroute.client.response.KakaoDirectionsResponse.Section;
import com.restroute.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.client.response.KakaoLocalSearchResponse;
import com.restroute.client.response.KakaoLocalSearchResponse.Document;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.route.dto.RouteRestStopCandidate;
import com.restroute.service.route.dto.RouteSearchResult;
import com.restroute.service.route.exception.RouteRestStopNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 카카오 좌표조회(목적지 해석, 길찾기 실패 안내, 경로 요약)와 경로 반경 안 후보 탐색만
 * 검증한다. 후보 정보 조합/응답 변환은 RouteRestStopServiceTest가 담당한다.
 */
@ExtendWith(MockitoExtension.class)
class RouteRestStopCandidateFinderTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5, 128.0, 38.0);

    @Mock
    private KakaoMapClient kakaoMapClient;

    @Mock
    private RestStopQueryService restStopQueryService;

    private RouteRestStopCandidateFinder finder;

    @BeforeEach
    void setUp() {
        finder = new RouteRestStopCandidateFinder(kakaoMapClient, restStopQueryService);
    }

    private KakaoLocalSearchResponse searchResult(String x, String y, String placeName, String addressName) {
        return new KakaoLocalSearchResponse(List.of(new Document(x, y, placeName, addressName)));
    }

    private KakaoDirectionsResponse directions(int code, Summary summary, List<Double> vertexes) {
        Route route = new Route(code, summary, List.of(new Section(List.of(new Road(vertexes)))));
        return new KakaoDirectionsResponse(List.of(route));
    }

    private RestStopEntity restStop(String code, String name, String route, String lng, String lat) {
        RestStopEntity entity = mock(RestStopEntity.class);
        lenient().when(entity.getServiceAreaCode()).thenReturn(code);
        lenient().when(entity.getUnitName()).thenReturn(name);
        lenient().when(entity.getRouteName()).thenReturn(route);
        lenient().when(entity.getXValue()).thenReturn(lng);
        lenient().when(entity.getYValue()).thenReturn(lat);
        return entity;
    }

    @Test
    @DisplayName("목적지 검색 결과가 없으면 NotFound (빈 리스트/ null 모두)")
    void emptySearch_throwsNotFound() {
        when(kakaoMapClient.searchKeyword("없는곳")).thenReturn(new KakaoLocalSearchResponse(List.of()));
        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "없는곳", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.searchKeyword("널")).thenReturn(new KakaoLocalSearchResponse(null));
        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "널", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("목적지 좌표를 해석하지 못하면 NotFound")
    void unparsableDestination_throwsNotFound() {
        when(kakaoMapClient.searchKeyword("경도없음")).thenReturn(searchResult(null, "35.0", "곳", null));
        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "경도없음", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.searchKeyword("위도없음")).thenReturn(searchResult("129.0", null, "곳", null));
        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "위도없음", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("길찾기에 성공 경로가 없으면 NotFound (result_code!=0, routes 비어있음/null)")
    void noSuccessfulRoute_throwsNotFound() {
        when(kakaoMapClient.searchKeyword(anyString())).thenReturn(searchResult("129.0", "35.0", "부산", null));

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(104, null, VERTEXES));
        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(new KakaoDirectionsResponse(List.of()));
        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(new KakaoDirectionsResponse(null));
        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("result_code별로 출발/도착/근접/기타 안내 메시지를 구분한다")
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
        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class)
                .hasMessageContaining(expectedFragment);
    }

    @Test
    @DisplayName("경로 좌표가 없으면 NotFound")
    void emptyPolyline_throwsNotFound() {
        when(kakaoMapClient.searchKeyword(anyString())).thenReturn(searchResult("129.0", "35.0", "부산", null));
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, List.of()));

        assertThatThrownBy(() -> finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("가장 가까운 도로 구간의 traffic_state로 후보의 인근 소통 상황을 채우고, 0이면 배지를 비운다")
    void success_addsNearbyTrafficFromNearestRoadSegment() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        Road jamRoad = new Road("경부선", 10L, 5L, 10, 1, List.of(127.0, 37.0));
        Road smoothRoad = new Road("경부선", 10L, 5L, 90, 4, List.of(127.5, 37.5));
        Road noInfoRoad = new Road("경부선", 10L, 5L, null, 0, List.of(128.0, 38.0));
        Route route =
                new Route(0, new Summary(100L, 200L), List.of(new Section(List.of(jamRoad, smoothRoad, noInfoRoad))));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(new KakaoDirectionsResponse(List.of(route)));

        RestStopEntity near0 = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity near1 = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        RestStopEntity near2 = restStop("C", "C휴게소", "경부선", "128.0001", "38.0001");
        when(restStopQueryService.findAll()).thenReturn(List.of(near0, near1, near2));

        RouteSearchResult result = finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000);

        List<RouteRestStopItem> items =
                result.candidates().stream().map(RouteRestStopCandidate::item).toList();
        assertThat(items.get(0).nearbyTraffic().key()).isEqualTo("jam");
        assertThat(items.get(0).nearbyTraffic().label()).isEqualTo("정체");
        assertThat(items.get(1).nearbyTraffic().key()).isEqualTo("smooth");
        assertThat(items.get(2).nearbyTraffic()).isNull();
    }

    @Test
    @DisplayName("위도 또는 경도 좌표가 없거나 반경을 벗어나면 후보에서 제외한다")
    void invalidOrFarRestStopCoordinates_excluded() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산", null));
        when(kakaoMapClient.getDirections(anyString(), anyString()))
                .thenReturn(directions(0, new Summary(1L, 1L), VERTEXES));
        RestStopEntity nullLatitude = restStop("A", "A", "x", "127.0", null);
        RestStopEntity nullLongitude = restStop("B", "B", "x", null, "37.0");
        RestStopEntity far = restStop("C", "C휴게소", "중부선", "130.0", "40.0");
        when(restStopQueryService.findAll()).thenReturn(List.of(nullLatitude, nullLongitude, far));

        RouteSearchResult result = finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(result.candidates()).isEmpty();
    }

    @Test
    @DisplayName("경로 1km 이내 휴게소만 경로 순서대로 후보에 포함한다")
    void success_filtersCandidatesWithinRadius() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity near0 = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity blank = restStop("D", "D", "x", "127.0", "   ");
        RestStopEntity nonNumeric = restStop("E", "E", "x", "127.0", "abc");
        when(restStopQueryService.findAll()).thenReturn(List.of(near0, blank, nonNumeric));

        RouteSearchResult result = finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(result.candidates())
                .extracting(candidate -> candidate.item().serviceAreaCode())
                .containsExactly("A");
    }

    @Test
    @DisplayName("summary가 null이면 거리/시간은 0, placeName이 비면 주소명을 이름으로 쓴다")
    void summaryNullAndAddressFallback() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "", "부산 우동"));
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));
        when(restStopQueryService.findAll()).thenReturn(List.of());

        RouteSearchResult result = finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(result.destination().name()).isEqualTo("부산 우동");
        assertThat(result.routeSummary().distanceMeters()).isZero();
        assertThat(result.routeSummary().durationSeconds()).isZero();
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    @DisplayName("목적지 좌표가 주어지면 지오코딩 없이 그 좌표로 경로를 계산한다")
    void destinationCoordinates_skipGeocoding() {
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(10L, 20L), VERTEXES));
        when(restStopQueryService.findAll()).thenReturn(List.of());

        RouteSearchResult result = finder.findCandidates(37.0, 127.0, null, 35.0, 129.0, "부산항", 1000);

        assertThat(result.destination().name()).isEqualTo("부산항");
        assertThat(result.destination().latitude()).isEqualTo(35.0);
        assertThat(result.destination().longitude()).isEqualTo(129.0);
        verify(kakaoMapClient, never()).searchKeyword(anyString());
    }

    @Test
    @DisplayName("목적지 좌표가 일부(경도만)면 query 지오코딩으로 폴백한다")
    void partialDestinationCoordinates_fallbackToQuery() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));
        when(restStopQueryService.findAll()).thenReturn(List.of());

        RouteSearchResult result = finder.findCandidates(37.0, 127.0, "부산", 35.0, null, "이름", 1000);

        assertThat(result.destination().name()).isEqualTo("부산역");
    }

    @Test
    @DisplayName("목적지 좌표만 있고 이름이 없거나 비면 기본 이름을 쓴다")
    void destinationCoordinates_defaultName() {
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));
        when(restStopQueryService.findAll()).thenReturn(List.of());

        assertThat(finder.findCandidates(37.0, 127.0, null, 35.0, 129.0, null, 1000)
                        .destination()
                        .name())
                .isEqualTo("목적지");
        assertThat(finder.findCandidates(37.0, 127.0, null, 35.0, 129.0, "  ", 1000)
                        .destination()
                        .name())
                .isEqualTo("목적지");
    }

    @Test
    @DisplayName("summary 값이 null이면 거리/시간 0으로 처리한다")
    void summaryWithNullValues() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산", null));
        when(kakaoMapClient.getDirections(anyString(), anyString()))
                .thenReturn(directions(0, new Summary(null, null), VERTEXES));
        when(restStopQueryService.findAll()).thenReturn(List.of());

        RouteSearchResult result = finder.findCandidates(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(result.routeSummary().distanceMeters()).isZero();
        assertThat(result.routeSummary().durationSeconds()).isZero();
    }
}
