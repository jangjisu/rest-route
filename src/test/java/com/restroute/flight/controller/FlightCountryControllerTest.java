package com.restroute.flight.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.flight.controller.response.FlightCountryResponse;
import com.restroute.flight.service.FlightCountryQueryService;
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
class FlightCountryControllerTest {

    @Mock
    private FlightCountryQueryService flightCountryQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightCountryController(flightCountryQueryService))
                .build();
    }

    @Test
    @DisplayName("GET /api/flights/countries는 keyword 파라미터를 그대로 서비스에 전달한다")
    void search_delegatesKeywordToService() throws Exception {
        FlightCountryResponse japan = new FlightCountryResponse("JP", "일본", "Japan");
        when(flightCountryQueryService.search("일본")).thenReturn(List.of(japan));

        mockMvc.perform(get("/api/flights/countries").param("keyword", "일본"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].code").value("JP"))
                .andExpect(jsonPath("$.data[0].korName").value("일본"))
                .andExpect(jsonPath("$.data[0].engName").value("Japan"));
    }

    @Test
    @DisplayName("파라미터가 없으면 null로 서비스에 위임한다")
    void search_delegatesNullKeyword_whenParamMissing() throws Exception {
        when(flightCountryQueryService.search(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/flights/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
