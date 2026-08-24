package com.restroute.controller;

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

@WebMvcTest(AnalyticsConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "google.analytics.measurement-id=G-TEST12345")
class AnalyticsConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/analytics-config는 GA4 측정 id를 반환한다")
    void getAnalyticsConfig_returnsMeasurementId() throws Exception {
        mockMvc.perform(get("/api/analytics-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.measurementId").value("G-TEST12345"));
    }
}
