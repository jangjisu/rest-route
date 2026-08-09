package com.restroute.flight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FlightSearchMockControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightSearchMockController())
                .build();
    }

    @Test
    @DisplayName("GET /api/flights/search/mock은 화면 시안과 동일한 고정 티켓 목록을 반환한다")
    void searchMock_returnsFixedSampleTickets() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalCount").value(128))
                .andExpect(jsonPath("$.data.tickets.length()").value(3))
                .andExpect(jsonPath("$.data.tickets[0].destinationCity").value("오사카 간사이"))
                .andExpect(jsonPath("$.data.tickets[0].bestPrice").value(true))
                .andExpect(jsonPath("$.data.tickets[0].price").value(198000))
                .andExpect(jsonPath("$.data.tickets[0].outbound.airline").value("진에어"))
                .andExpect(jsonPath("$.data.tickets[2].transferCount").value(1))
                .andExpect(
                        jsonPath("$.data.tickets[2].outbound.transferAirport").value("FUK"));
    }
}
