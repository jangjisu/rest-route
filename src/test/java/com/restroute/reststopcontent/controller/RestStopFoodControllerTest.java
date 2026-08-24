package com.restroute.reststopcontent.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.reststopcontent.controller.response.FoodMenuItemResponse;
import com.restroute.reststopcontent.controller.response.FoodMenuResponse;
import com.restroute.reststopcontent.controller.response.FoodMenuSectionResponse;
import com.restroute.reststopcontent.service.RestStopFoodMenuQueryService;
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
class RestStopFoodControllerTest {

    @Mock
    private RestStopFoodMenuQueryService restStopFoodMenuQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RestStopFoodController(restStopFoodMenuQueryService))
                .build();
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}/foods는 음식 메뉴 정보를 ApiResponse로 반환한다")
    void getRestStopFoods_returnsFoodMenu() throws Exception {
        FoodMenuResponse response = new FoodMenuResponse(
                List.of(new FoodMenuItemResponse("농심어묵우동", "7000", "시원한 우동", true, true, false, "S", "여름")),
                List.of(new FoodMenuSectionResponse(
                        "recommended",
                        "추천 메뉴",
                        List.of(new FoodMenuItemResponse("농심어묵우동", "7000", "시원한 우동", true, true, false, "S", "여름")))));
        when(restStopFoodMenuQueryService.findByServiceAreaCode("A00001")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/rest-stops/A00001/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.menus[0].foodName").value("농심어묵우동"))
                .andExpect(jsonPath("$.data.menus[0].foodCost").value("7000"))
                .andExpect(jsonPath("$.data.menus[0].recommended").value(true))
                .andExpect(jsonPath("$.data.menus[0].bestFood").value(true))
                .andExpect(jsonPath("$.data.menus[0].seasonLabel").value("여름"))
                .andExpect(jsonPath("$.data.sections[0].key").value("recommended"))
                .andExpect(jsonPath("$.data.sections[0].menus[0].foodName").value("농심어묵우동"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}/foods는 대상 휴게소가 없으면 NOT_FOUND를 반환한다")
    void getRestStopFoods_returnsNotFoundWhenRestStopMissing() throws Exception {
        when(restStopFoodMenuQueryService.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rest-stops/UNKNOWN/foods"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }
}
