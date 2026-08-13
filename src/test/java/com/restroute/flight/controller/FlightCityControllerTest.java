package com.restroute.flight.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.flight.controller.response.FlightCityResponse;
import com.restroute.flight.service.FlightCityQueryService;
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
class FlightCityControllerTest {

    @Mock
    private FlightCityQueryService flightCityQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightCityController(flightCityQueryService))
                .build();
    }

    @Test
    @DisplayName("GET /api/flights/cities는 keyword 파라미터를 그대로 서비스에 전달한다")
    void search_delegatesKeywordToService() throws Exception {
        FlightCityResponse osaka = new FlightCityResponse("OSA", "오사카", "Osaka", "JP");
        when(flightCityQueryService.search("오사카")).thenReturn(List.of(osaka));

        mockMvc.perform(get("/api/flights/cities").param("keyword", "오사카"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].code").value("OSA"))
                .andExpect(jsonPath("$.data[0].korName").value("오사카"))
                .andExpect(jsonPath("$.data[0].engName").value("Osaka"));
    }

    @Test
    @DisplayName("파라미터가 없으면 null로 서비스에 위임한다")
    void search_delegatesNullKeyword_whenParamMissing() throws Exception {
        when(flightCityQueryService.search(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/flights/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
