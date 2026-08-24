package com.restroute.reststop.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.reststop.controller.response.RestStopSalesRankingItemResponse;
import com.restroute.reststop.controller.response.RestStopSalesRankingResponse;
import com.restroute.reststop.controller.response.RestStopSalesRankingStoreItemResponse;
import com.restroute.reststop.service.RestStopSalesRankingQueryService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RestStopSalesRankingControllerTest {

    @Mock
    private RestStopSalesRankingQueryService queryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RestStopSalesRankingController(queryService))
                .build();
    }

    @Test
    void returnsSalesRankingResponse() throws Exception {
        RestStopSalesRankingResponse response = new RestStopSalesRankingResponse(
                "2026-06",
                List.of(new RestStopSalesRankingStoreItemResponse(1, "CU편의점")),
                List.of(new RestStopSalesRankingItemResponse(1, "대표 메뉴")));
        when(queryService.findByServiceAreaCode("A00001")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/rest-stops/A00001/sales-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.baseYearMonth").value("2026-06"))
                .andExpect(jsonPath("$.data.storeRankings[0].rank").value(1))
                .andExpect(jsonPath("$.data.storeRankings[0].storeName").value("CU편의점"))
                .andExpect(jsonPath("$.data.products[0].rank").value(1))
                .andExpect(jsonPath("$.data.products[0].productName").value("대표 메뉴"));
    }

    @Test
    void returnsNotFoundWhenRestStopDoesNotExist() throws Exception {
        when(queryService.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rest-stops/UNKNOWN/sales-rankings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
