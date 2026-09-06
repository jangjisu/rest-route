package com.restroute.oilprice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.oilprice.dto.AverageOilPrice;
import com.restroute.oilprice.dto.NationalOilPriceSummary;
import com.restroute.oilprice.service.NationalOilPriceService;
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
class NationalOilPriceControllerTest {

    @Mock
    private NationalOilPriceService nationalOilPriceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NationalOilPriceController(nationalOilPriceService))
                .build();
    }

    @Test
    @DisplayName("GET /api/national-oil-prices/summary는 전국 유가 요약을 ApiResponse로 반환한다")
    void getNationalOilPriceSummary_returnsSummary() throws Exception {
        NationalOilPriceSummary response = NationalOilPriceSummary.of(
                "2026.07.07",
                AverageOilPrice.of("B027", "휘발유", "1,893원", "-4.19"),
                AverageOilPrice.of("D047", "자동차용경유", "1,880원", "-4.51"),
                AverageOilPrice.of("K015", "자동차용부탄", "1,135원", "+0.01"));
        when(nationalOilPriceService.getTodaySummary()).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/national-oil-prices/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.tradeDate").value("2026.07.07"))
                .andExpect(jsonPath("$.data.gasoline.productCode").value("B027"))
                .andExpect(jsonPath("$.data.gasoline.productName").value("휘발유"))
                .andExpect(jsonPath("$.data.gasoline.price").value("1,893원"))
                .andExpect(jsonPath("$.data.gasoline.dailyDiff").value("-4.19"))
                .andExpect(jsonPath("$.data.diesel.productCode").value("D047"))
                .andExpect(jsonPath("$.data.lpg.productCode").value("K015"));
    }

    @Test
    @DisplayName("GET /api/national-oil-prices/summary는 요약을 만들 수 없으면 EXTERNAL_API_UNAVAILABLE을 반환한다")
    void getNationalOilPriceSummary_returnsExternalUnavailableWhenSummaryMissing() throws Exception {
        when(nationalOilPriceService.getTodaySummary()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/national-oil-prices/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("External API temporarily unavailable"));
    }
}
