package com.restroute.oilprice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.oilprice.controller.response.OilInfoResponse;
import com.restroute.oilprice.controller.response.OilStationConvenienceResponse;
import com.restroute.oilprice.service.RestOilPriceRefreshService;
import com.restroute.oilprice.service.RestStopOilInfoQueryService;
import java.time.LocalDateTime;
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
class RestStopOilInfoControllerTest {

    @Mock
    private RestStopOilInfoQueryService restStopOilInfoQueryService;

    @Mock
    private RestOilPriceRefreshService restOilPriceRefreshService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new RestStopOilInfoController(restStopOilInfoQueryService, restOilPriceRefreshService))
                .build();
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}/oil-info는 주유 정보를 ApiResponse로 반환한다")
    void getRestStopOilInfo_returnsOilInfo() throws Exception {
        OilInfoResponse response = new OilInfoResponse(
                "AD",
                "1,999원",
                "1,997원",
                "1,157원",
                "02-573-7430",
                LocalDateTime.of(2026, 6, 16, 7, 30),
                List.of(new OilStationConvenienceResponse("00:00", "24:00", "쉼터", "고객쉼터")));
        when(restStopOilInfoQueryService.findByServiceAreaCode("A00001")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/rest-stops/A00001/oil-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.oilCompany").value("AD"))
                .andExpect(jsonPath("$.data.gasolinePrice").value("1,999원"))
                .andExpect(jsonPath("$.data.dieselPrice").value("1,997원"))
                .andExpect(jsonPath("$.data.lpgPrice").value("1,157원"))
                .andExpect(jsonPath("$.data.telNo").value("02-573-7430"))
                .andExpect(jsonPath("$.data.lastRefreshedAt").value("2026-06-16T07:30:00"))
                .andExpect(jsonPath("$.data.oilStationConveniences[0].name").value("쉼터"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}/oil-info는 주유 정보가 없으면 NOT_FOUND를 반환한다")
    void getRestStopOilInfo_returnsNotFoundWhenOilInfoMissing() throws Exception {
        when(restStopOilInfoQueryService.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rest-stops/UNKNOWN/oil-info"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @DisplayName("POST /api/rest-stops/{serviceAreaCode}/oil-price/refresh는 주유 가격을 갱신하고 oilInfo를 반환한다")
    void refreshRestOilPrice_returnsOilInfo() throws Exception {
        OilInfoResponse response = new OilInfoResponse(
                "AD",
                "1,888원",
                "1,777원",
                "X",
                "02-573-7430",
                LocalDateTime.of(2026, 6, 16, 7, 40),
                List.of(new OilStationConvenienceResponse("00:00", "24:00", "쉼터", "고객쉼터")));
        when(restOilPriceRefreshService.refreshByServiceAreaCode("A00001")).thenReturn(Optional.of(response));

        mockMvc.perform(post("/api/rest-stops/A00001/oil-price/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.oilCompany").value("AD"))
                .andExpect(jsonPath("$.data.gasolinePrice").value("1,888원"))
                .andExpect(jsonPath("$.data.dieselPrice").value("1,777원"))
                .andExpect(jsonPath("$.data.lpgPrice").value("X"))
                .andExpect(jsonPath("$.data.telNo").value("02-573-7430"))
                .andExpect(jsonPath("$.data.lastRefreshedAt").value("2026-06-16T07:40:00"))
                .andExpect(jsonPath("$.data.oilStationConveniences[0].name").value("쉼터"));
    }

    @Test
    @DisplayName("POST /api/rest-stops/{serviceAreaCode}/oil-price/refresh는 갱신 대상이 없으면 NOT_FOUND를 반환한다")
    void refreshRestOilPrice_returnsNotFoundWhenTargetMissing() throws Exception {
        when(restOilPriceRefreshService.refreshByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/rest-stops/UNKNOWN/oil-price/refresh"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }
}
