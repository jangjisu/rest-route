package com.restroute.controller;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.common.GlobalExceptionHandler;
import com.restroute.controller.response.RestStopCompareResponse;
import com.restroute.controller.response.RestStopCompareResponse.RestStopCompareResult;
import com.restroute.controller.response.RestStopCompareResponse.RestStopCompareSide;
import com.restroute.controller.response.RestStopDetailViewResponse;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.compare.InvalidRestStopCompareException;
import com.restroute.service.compare.RestStopCompareService;
import com.restroute.service.image.RestStopNotFoundException;
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
class RestStopControllerTest {

    @Mock
    private RestStopQueryService restStopQueryService;

    @Mock
    private RestStopCompareService restStopCompareService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RestStopController(restStopQueryService, restStopCompareService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/rest-stops는 휴게소 목록을 ApiResponse로 반환한다")
    void getRestStops_returnsRestStops() throws Exception {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));

        mockMvc.perform(get("/api/rest-stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].unitCode").value("001"))
                .andExpect(jsonPath("$.data[0].unitName").value("서울만남(부산)휴게소"))
                .andExpect(jsonPath("$.data[0].routeNo").value("0010"))
                .andExpect(jsonPath("$.data[0].routeName").value("경부선"))
                .andExpect(jsonPath("$.data[0].xValue").value("127.042514"))
                .andExpect(jsonPath("$.data[0].yValue").value("37.459939"))
                .andExpect(jsonPath("$.data[0].stdRestCd").value("000001"))
                .andExpect(jsonPath("$.data[0].serviceAreaCode").value("A00001"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}는 휴게소 상세 정보를 ApiResponse로 반환한다")
    void getRestStopDetail_returnsRestStopDetail() throws Exception {
        RestStopDetailViewResponse response = new RestStopDetailViewResponse(
                "A00001", "001", "서울만남(부산)휴게소", "0010", "경부선", "127.042514", "37.459939", "000001");
        when(restStopQueryService.findDetailByServiceAreaCode("A00001")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/rest-stops/A00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.serviceAreaCode").value("A00001"))
                .andExpect(jsonPath("$.data.unitCode").value("001"))
                .andExpect(jsonPath("$.data.unitName").value("서울만남(부산)휴게소"))
                .andExpect(jsonPath("$.data.routeNo").value("0010"))
                .andExpect(jsonPath("$.data.routeName").value("경부선"))
                .andExpect(jsonPath("$.data.xValue").value("127.042514"))
                .andExpect(jsonPath("$.data.yValue").value("37.459939"))
                .andExpect(jsonPath("$.data.stdRestCd").value("000001"))
                .andExpect(jsonPath("$.data.oilInfo").doesNotExist())
                .andExpect(jsonPath("$.data.foodMenu").doesNotExist())
                .andExpect(jsonPath("$.data.convenience").doesNotExist())
                .andExpect(jsonPath("$.data.compactCarParkingCount").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/rest-stops/{serviceAreaCode}는 대상 휴게소가 없으면 NOT_FOUND를 반환한다")
    void getRestStopDetail_returnsNotFoundWhenRestStopMissing() throws Exception {
        when(restStopQueryService.findDetailByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rest-stops/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/search는 이름으로 검색한 휴게소 목록을 ApiResponse로 반환한다")
    void searchRestStops_returnsMatchingRestStops() throws Exception {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopQueryService.searchByName("서울만남")).thenReturn(List.of(restStop));

        mockMvc.perform(get("/api/rest-stops/search").param("name", "서울만남"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].unitName").value("서울만남(부산)휴게소"))
                .andExpect(jsonPath("$.data[0].serviceAreaCode").value("A00001"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/search는 일치하는 휴게소가 없으면 빈 배열을 반환한다")
    void searchRestStops_returnsEmptyArrayWhenNoMatch() throws Exception {
        when(restStopQueryService.searchByName("없는이름")).thenReturn(List.of());

        mockMvc.perform(get("/api/rest-stops/search").param("name", "없는이름"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /api/rest-stops/compare는 두 휴게소의 비교 결과를 ApiResponse로 반환한다")
    void compareRestStops_returnsComparisonResult() throws Exception {
        RestStopCompareSide sideA = RestStopCompareSide.of(
                "A00001",
                "888안성(서울)휴게소",
                "경부선",
                "/api/rest-stops/A00001/images/list",
                "1798",
                "1689",
                "1186",
                312,
                List.of("수유실", "샤워실"));
        RestStopCompareSide sideB =
                RestStopCompareSide.of("A00002", "죽전(부산)복합휴게소", "경부선", null, "1872", "1720", "1140", 201, List.of());
        RestStopCompareResult result = RestStopCompareResult.of("A", "A", "B", "A", "A", "A");
        when(restStopCompareService.compare("A00001", "A00002"))
                .thenReturn(RestStopCompareResponse.of(sideA, sideB, result));

        mockMvc.perform(get("/api/rest-stops/compare")
                        .param("serviceAreaCodeA", "A00001")
                        .param("serviceAreaCodeB", "A00002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sideA.unitName").value("888안성(서울)휴게소"))
                .andExpect(jsonPath("$.data.sideB.unitName").value("죽전(부산)복합휴게소"))
                .andExpect(jsonPath("$.data.result.recommendedSide").value("A"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/compare는 대상 휴게소가 없으면 NOT_FOUND를 반환한다")
    void compareRestStops_returnsNotFoundWhenRestStopMissing() throws Exception {
        when(restStopCompareService.compare("A00001", "UNKNOWN"))
                .thenThrow(RestStopNotFoundException.forServiceAreaCode("UNKNOWN"));

        mockMvc.perform(get("/api/rest-stops/compare")
                        .param("serviceAreaCodeA", "A00001")
                        .param("serviceAreaCodeB", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/rest-stops/compare는 같은 휴게소를 두 번 넣으면 INVALID_PARAMETER를 반환한다")
    void compareRestStops_returnsInvalidParameterWhenSameServiceAreaCode() throws Exception {
        when(restStopCompareService.compare("A00001", "A00001"))
                .thenThrow(InvalidRestStopCompareException.forSameServiceAreaCode("A00001"));

        mockMvc.perform(get("/api/rest-stops/compare")
                        .param("serviceAreaCodeA", "A00001")
                        .param("serviceAreaCodeB", "A00001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}
