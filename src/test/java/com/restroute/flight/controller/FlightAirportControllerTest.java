package com.restroute.flight.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.flight.controller.response.FlightAirportResponse;
import com.restroute.flight.service.FlightAirportQueryService;
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
class FlightAirportControllerTest {

    @Mock
    private FlightAirportQueryService flightAirportQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightAirportController(flightAirportQueryService))
                .build();
    }

    @Test
    @DisplayName("GET /api/flights/airports는 keyword 파라미터를 그대로 서비스에 전달한다")
    void search_delegatesKeywordToService() throws Exception {
        FlightAirportResponse icn =
                new FlightAirportResponse("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR");
        when(flightAirportQueryService.search("인천")).thenReturn(List.of(icn));

        mockMvc.perform(get("/api/flights/airports").param("keyword", "인천"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].code").value("ICN"))
                .andExpect(jsonPath("$.data[0].korName").value("인천국제공항"))
                .andExpect(jsonPath("$.data[0].cityCode").value("SEL"));
    }

    @Test
    @DisplayName("파라미터가 없으면 null로 서비스에 위임한다")
    void search_delegatesNullKeyword_whenParamMissing() throws Exception {
        when(flightAirportQueryService.search(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/flights/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
