package com.restroute.reststop.controller;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.reststop.controller.response.RestStopDetailViewResponse;
import com.restroute.reststop.controller.response.RestStopNearbyItemResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.RestStopNearbyQueryService;
import com.restroute.reststop.service.RestStopQueryService;
import com.restroute.reststop.service.dto.RestStopInterest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RestStopControllerTest {

    @Mock
    private RestStopQueryService restStopQueryService;

    @Mock
    private RestStopNearbyQueryService restStopNearbyQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new RestStopController(restStopQueryService, restStopNearbyQueryService))
                .build();
    }

    @Test
    @DisplayName("GET /api/rest-stops는 휴게소 목록을 ApiResponse로 반환한다")
    void getRestStops_returnsRestStops() throws Exception {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));

        mockMvc.perform(get("/api/rest-stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].unitCode").value("001"))
                .andExpect(jsonPath("$.data[0].unitName").value("서울만남(부산)휴게소"))
                .andExpect(jsonPath("$.data[0].routeNo").value("0010"))
                .andExpect(jsonPath("$.data[0].routeName").value("경부선"))
                .andExpect(jsonPath("$.data[0].xValue").value("127.042514"))
                .andExpect(jsonPath("$.data[0].yValue").value("37.459939"))
                .andExpect(jsonPath("$.data[0].stdRestCd").value("000001"))
                .andExpect(jsonPath("$.data[0].serviceAreaCode").value("A00001"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}는 휴게소 상세 정보를 ApiResponse로 반환한다")
    void getRestStopDetail_returnsRestStopDetail() throws Exception {
        RestStopDetailViewResponse response = new RestStopDetailViewResponse(
                "A00001", "001", "서울만남(부산)휴게소", "0010", "경부선", "127.042514", "37.459939", "000001");
        when(restStopQueryService.findDetailByServiceAreaCode("A00001")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/rest-stops/A00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.serviceAreaCode").value("A00001"))
                .andExpect(jsonPath("$.data.unitCode").value("001"))
                .andExpect(jsonPath("$.data.unitName").value("서울만남(부산)휴게소"))
                .andExpect(jsonPath("$.data.routeNo").value("0010"))
                .andExpect(jsonPath("$.data.routeName").value("경부선"))
                .andExpect(jsonPath("$.data.xValue").value("127.042514"))
                .andExpect(jsonPath("$.data.yValue").value("37.459939"))
                .andExpect(jsonPath("$.data.stdRestCd").value("000001"))
                .andExpect(jsonPath("$.data.oilInfo").doesNotExist())
                .andExpect(jsonPath("$.data.foodMenu").doesNotExist())
                .andExpect(jsonPath("$.data.convenience").doesNotExist())
                .andExpect(jsonPath("$.data.compactCarParkingCount").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}는 대상 휴게소가 없으면 NOT_FOUND를 반환한다")
    void getRestStopDetail_returnsNotFoundWhenRestStopMissing() throws Exception {
        when(restStopQueryService.findDetailByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rest-stops/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/search는 이름으로 검색한 휴게소 목록을 ApiResponse로 반환한다")
    void searchRestStops_returnsMatchingRestStops() throws Exception {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopQueryService.searchByName("서울만남")).thenReturn(List.of(restStop));

        mockMvc.perform(get("/api/rest-stops/search").param("name", "서울만남"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].unitName").value("서울만남(부산)휴게소"))
                .andExpect(jsonPath("$.data[0].serviceAreaCode").value("A00001"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/search는 일치하는 휴게소가 없으면 빈 배열을 반환한다")
    void searchRestStops_returnsEmptyArrayWhenNoMatch() throws Exception {
        when(restStopQueryService.searchByName("없는이름")).thenReturn(List.of());

        mockMvc.perform(get("/api/rest-stops/search").param("name", "없는이름"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /api/rest-stops/nearby는 파라미터를 그대로 서비스에 넘기고 결과를 반환한다")
    void getNearbyRestStops_delegatesToServiceWithParsedParams() throws Exception {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestStopNearbyItemResponse response =
                RestStopNearbyItemResponse.of(restStop, 1200.0, null, true, false, false, null, null);
        when(restStopNearbyQueryService.findNearby(37.5, 127.0, "안성", RestStopInterest.EV))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/rest-stops/nearby")
                        .param("originLat", "37.5")
                        .param("originLng", "127.0")
                        .param("name", "안성")
                        .param("interest", "EV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].unitName").value("서울만남(부산)휴게소"))
                .andExpect(jsonPath("$.data[0].distanceMeters").value(1200.0))
                .andExpect(jsonPath("$.data[0].topTrafficTier").value(true));
    }

    @Test
    @DisplayName("GET /api/rest-stops/nearby는 파라미터가 하나도 없어도 동작한다")
    void getNearbyRestStops_worksWithoutAnyParams() throws Exception {
        when(restStopNearbyQueryService.findNearby(null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/rest-stops/nearby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray());
    }
}
