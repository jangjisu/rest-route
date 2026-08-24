package com.restroute.placesearch.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.common.GlobalExceptionHandler;
import com.restroute.common.client.exception.KakaoApiException;
import com.restroute.placesearch.controller.response.PlaceCandidateResponse;
import com.restroute.placesearch.service.PlaceSearchService;
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
class PlaceSearchControllerTest {

    @Mock
    private PlaceSearchService placeSearchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PlaceSearchController(placeSearchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/place-search는 후보 목록을 ApiResponse로 반환한다")
    void search_returnsCandidates() throws Exception {
        when(placeSearchService.search("부산"))
                .thenReturn(List.of(PlaceCandidateResponse.of("부산역", "부산 동구", 35.11, 129.04)));

        mockMvc.perform(get("/api/place-search").param("query", "부산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].name").value("부산역"))
                .andExpect(jsonPath("$.data[0].latitude").value(35.11))
                .andExpect(jsonPath("$.data[0].longitude").value(129.04));
    }

    @Test
    @DisplayName("카카오 호출 실패 시 EXTERNAL_API_UNAVAILABLE를 반환한다")
    void search_externalFailure() throws Exception {
        when(placeSearchService.search(anyString())).thenThrow(new KakaoApiException("local", "boom"));

        mockMvc.perform(get("/api/place-search").param("query", "부산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_UNAVAILABLE"));
    }
}
