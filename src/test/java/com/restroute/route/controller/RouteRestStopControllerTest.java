package com.restroute.route.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.common.GlobalExceptionHandler;
import com.restroute.common.client.exception.KakaoApiException;
import com.restroute.route.controller.response.RouteRestStopListItemResponse;
import com.restroute.route.controller.response.RouteRestStopResponse;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteOption;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteSummary;
import com.restroute.route.service.RouteRestStopListQueryService;
import com.restroute.route.service.RouteRestStopService;
import com.restroute.route.service.dto.FuelType;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RouteRestStopControllerTest {

    @Mock
    private RouteRestStopService routeRestStopService;

    @Mock
    private RouteRestStopListQueryService routeRestStopListQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new RouteRestStopController(routeRestStopService, routeRestStopListQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/route-rest-stops는 경로상 휴게소를 ApiResponse로 반환한다")
    void getRouteRestStops_returnsRouteRestStops() throws Exception {
        RouteRestStopResponse response = new RouteRestStopResponse(
                new Destination("부산역", 35.0, 129.0),
                List.of(new RouteOption(
                        0,
                        new RouteSummary(100L, 200L, 4500L, List.of(List.of(127.0, 37.0))),
                        List.of(new RouteRestStopItem(
                                "A",
                                "A휴게소",
                                "경부선",
                                37.0,
                                127.0,
                                false,
                                false,
                                false,
                                false,
                                12L,
                                RouteRestStopResponse.ComparisonSummary.empty(),
                                List.of(),
                                "/api/rest-stops/A/images/list",
                                null,
                                null,
                                null,
                                false,
                                null,
                                null)))));
        when(routeRestStopService.findRouteRestStops(eq(37.0), eq(127.0), eq("부산"), any(), any(), any(), eq(1000)))
                .thenReturn(response);

        mockMvc.perform(get("/api/route-rest-stops")
                        .param("originLat", "37.0")
                        .param("originLng", "127.0")
                        .param("destinationQuery", "부산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.destination.name").value("부산역"))
                .andExpect(jsonPath("$.data.routes[0].summary.distanceMeters").value(100))
                .andExpect(jsonPath("$.data.routes[0].summary.tollFareWon").value(4500))
                .andExpect(jsonPath("$.data.nationalOilPriceSummary").doesNotExist())
                .andExpect(jsonPath("$.data.routes[0].restStops[0].serviceAreaCode")
                        .value("A"))
                .andExpect(jsonPath("$.data.routes[0].restStops[0].hasDirectionAlternative")
                        .value(false))
                .andExpect(jsonPath("$.data.routes[0].restStops[0].distanceFromRouteMeters")
                        .value(12))
                .andExpect(
                        jsonPath("$.data.routes[0].restStops[0].listImageUrl").value("/api/rest-stops/A/images/list"));
    }

    @Test
    @DisplayName("GET /api/route-rest-stops는 목록 이미지가 없으면 null 목록 이미지 URL을 반환한다")
    void getRouteRestStops_returnsNullListImageUrlWhenImageIsMissing() throws Exception {
        RouteRestStopResponse response = new RouteRestStopResponse(
                new Destination("부산역", 35.0, 129.0),
                List.of(new RouteOption(
                        0,
                        new RouteSummary(100L, 200L, 0L, List.of(List.of(127.0, 37.0))),
                        List.of(new RouteRestStopItem("A", "A휴게소", "경부선", 37.0, 127.0, 12L)))));
        when(routeRestStopService.findRouteRestStops(eq(37.0), eq(127.0), eq("부산"), any(), any(), any(), eq(1000)))
                .thenReturn(response);

        mockMvc.perform(get("/api/route-rest-stops")
                        .param("originLat", "37.0")
                        .param("originLng", "127.0")
                        .param("destinationQuery", "부산"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.routes[0].restStops[0].listImageUrl").value(nullValue()));
    }

    @Test
    @DisplayName("목적지를 찾지 못하면 404 NOT_FOUND를 반환한다")
    void notFound_returns404() throws Exception {
        when(routeRestStopService.findRouteRestStops(
                        anyDouble(), anyDouble(), anyString(), any(), any(), any(), anyInt()))
                .thenThrow(new RouteRestStopNotFoundException("없음"));

        mockMvc.perform(get("/api/route-rest-stops")
                        .param("originLat", "37.0")
                        .param("originLng", "127.0")
                        .param("destinationQuery", "없는곳"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("카카오 호출 실패 시 EXTERNAL_API_UNAVAILABLE를 반환한다")
    void kakaoFailure_returnsExternalUnavailable() throws Exception {
        when(routeRestStopService.findRouteRestStops(
                        anyDouble(), anyDouble(), anyString(), any(), any(), any(), anyInt()))
                .thenThrow(new KakaoApiException("directions", "boom"));

        mockMvc.perform(get("/api/route-rest-stops")
                        .param("originLat", "37.0")
                        .param("originLng", "127.0")
                        .param("destinationQuery", "부산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_UNAVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/route-rest-stops/list는 거리·유가 등급이 붙은 목록을 ApiResponse로 반환한다")
    void getRouteRestStopList_returnsListWithParsedFuelType() throws Exception {
        List<RouteRestStopListItemResponse> response =
                List.of(RouteRestStopListItemResponse.of("A", "A휴게소", "경부선", 850.5, null, true, 3, null));
        when(routeRestStopListQueryService.findRouteRestStops(
                        eq(37.0), eq(127.0), eq("부산"), any(), any(), any(), eq(1000), eq(FuelType.DIESEL)))
                .thenReturn(response);

        mockMvc.perform(get("/api/route-rest-stops/list")
                        .param("originLat", "37.0")
                        .param("originLng", "127.0")
                        .param("destinationQuery", "부산")
                        .param("fuelType", "DIESEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].serviceAreaCode").value("A"))
                .andExpect(jsonPath("$.data[0].distanceMeters").value(850.5))
                .andExpect(jsonPath("$.data[0].evChargerCount").value(3));
    }

    @Test
    @DisplayName("GET /api/route-rest-stops/list는 fuelType 없이도 동작한다")
    void getRouteRestStopList_worksWithoutFuelType() throws Exception {
        when(routeRestStopListQueryService.findRouteRestStops(
                        eq(37.0), eq(127.0), eq("부산"), any(), any(), any(), eq(1000), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/route-rest-stops/list")
                        .param("originLat", "37.0")
                        .param("originLng", "127.0")
                        .param("destinationQuery", "부산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
