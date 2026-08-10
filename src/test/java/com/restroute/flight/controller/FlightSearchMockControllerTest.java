package com.restroute.flight.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.common.GlobalExceptionHandler;
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
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/flights/search/mock은 화면 시안과 동일한 고정 티켓 목록을 반환한다")
    void searchMock_returnsFixedSampleTickets() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", "ICN")
                        .param("destination", "OSA")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-08-31")
                        .param("regions", "JAPAN")
                        .param("nights", "3", "4"))
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

    @Test
    @DisplayName("도착지/지역권 없이도(옵션) 요청이 성공한다")
    void searchMock_succeedsWithoutOptionalParams() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", "ICN")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31")
                        .param("nights", "3"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dateTo가 dateFrom보다 빠르면 INVALID_PARAMETER를 반환한다")
    void searchMock_rejectsReversedDateRange() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", "ICN")
                        .param("dateFrom", "2026-08-01")
                        .param("dateTo", "2026-07-01")
                        .param("nights", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("날짜 범위가 3개월을 넘으면 INVALID_PARAMETER를 반환한다")
    void searchMock_rejectsDateRangeOverThreeMonths() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", "ICN")
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-06-01")
                        .param("nights", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("nights가 비어있으면 INVALID_PARAMETER를 반환한다")
    void searchMock_rejectsMissingNights() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", "ICN")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("regions에 알 수 없는 값이 있으면 INVALID_PARAMETER를 반환한다")
    void searchMock_rejectsUnknownRegion() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", "ICN")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31")
                        .param("nights", "3")
                        .param("regions", "EUROPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("origin이 없으면 INVALID_PARAMETER를 반환한다")
    void searchMock_rejectsMissingOrigin() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31")
                        .param("nights", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}
