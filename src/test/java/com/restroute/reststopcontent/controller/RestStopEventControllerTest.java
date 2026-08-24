package com.restroute.reststopcontent.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.reststopcontent.controller.response.RestStopEventResponse;
import com.restroute.reststopcontent.controller.response.RestStopEventResponse.EventInfo;
import com.restroute.reststopcontent.service.RestStopEventQueryService;
import java.util.List;
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
class RestStopEventControllerTest {

    @Mock
    private RestStopEventQueryService restStopEventQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RestStopEventController(restStopEventQueryService))
                .build();
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}/events는 진행 중인 이벤트 정보를 ApiResponse로 반환한다")
    void getRestStopEvents_returnsEvents() throws Exception {
        RestStopEventResponse response = new RestStopEventResponse(
                List.of(new EventInfo("TEN+1 이벤트", "한식당 식사 10번 이용 시 1번 무료", "2026.01.01 ~ 2026.12.31")));
        when(restStopEventQueryService.findByServiceAreaCode("A00001")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/rest-stops/A00001/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.events[0].name").value("TEN+1 이벤트"))
                .andExpect(jsonPath("$.data.events[0].detail").value("한식당 식사 10번 이용 시 1번 무료"))
                .andExpect(jsonPath("$.data.events[0].period").value("2026.01.01 ~ 2026.12.31"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}/events는 대상 휴게소가 없으면 NOT_FOUND를 반환한다")
    void getRestStopEvents_returnsNotFoundWhenRestStopMissing() throws Exception {
        when(restStopEventQueryService.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rest-stops/UNKNOWN/events"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }
}
