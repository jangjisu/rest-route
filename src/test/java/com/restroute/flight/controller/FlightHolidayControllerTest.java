package com.restroute.flight.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.flight.controller.response.FlightHolidayResponse;
import com.restroute.flight.service.FlightHolidayQueryService;
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
class FlightHolidayControllerTest {

    @Mock
    private FlightHolidayQueryService flightHolidayQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightHolidayController(flightHolidayQueryService))
                .build();
    }

    @Test
    @DisplayName("months 파라미터가 없으면 null로 서비스에 위임한다")
    void findAll_delegatesNullMonths_whenParamMissing() throws Exception {
        FlightHolidayResponse newYear = new FlightHolidayResponse("2026-01-01", "신정");
        when(flightHolidayQueryService.findAll(null)).thenReturn(List.of(newYear));

        mockMvc.perform(get("/api/flights/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].date").value("2026-01-01"))
                .andExpect(jsonPath("$.data[0].name").value("신정"));
    }

    @Test
    @DisplayName("months를 여러 개 주면 그 목록 그대로 서비스에 전달한다")
    void findAll_delegatesMonthListToService() throws Exception {
        FlightHolidayResponse chuseok = new FlightHolidayResponse("2026-09-25", "추석");
        when(flightHolidayQueryService.findAll(List.of(9, 10))).thenReturn(List.of(chuseok));

        mockMvc.perform(get("/api/flights/holidays").param("months", "9", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].date").value("2026-09-25"))
                .andExpect(jsonPath("$.data[0].name").value("추석"));
    }
}
