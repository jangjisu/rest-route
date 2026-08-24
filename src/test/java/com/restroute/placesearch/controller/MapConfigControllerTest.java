package com.restroute.placesearch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MapConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "naver.maps.ncp-key-id=test-map-key")
class MapConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/map-config는 네이버 지도 key id를 반환한다")
    void getMapConfig_returnsNaverMapsKeyId() throws Exception {
        mockMvc.perform(get("/api/map-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.naverMapsNcpKeyId").value("test-map-key"));
    }
}
