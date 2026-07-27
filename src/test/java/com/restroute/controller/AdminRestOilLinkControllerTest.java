package com.restroute.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.common.GlobalExceptionHandler;
import com.restroute.controller.request.AdminOilStationLinkRequest;
import com.restroute.controller.response.AdminOilStationLinkResponse;
import com.restroute.controller.response.AdminOilStationSearchResponse;
import com.restroute.controller.response.AdminRestOilLinkSummaryResponse;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admin.AdminRestOilLinkService;
import com.restroute.service.admin.RestOilNotFoundException;
import com.restroute.service.image.RestStopNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminRestOilLinkControllerTest {

    @Mock
    private AdminRestOilLinkService adminRestOilLinkService;

    @Mock
    private AdminActivityLogService adminActivityLogService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminRestOilLinkController(adminRestOilLinkService, adminActivityLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/rest-stops/oil-links는 전체 휴게소별 연결 목록을 반환한다")
    void findAll_returnsOk() throws Exception {
        when(adminRestOilLinkService.findAll())
                .thenReturn(List.of(new AdminRestOilLinkSummaryResponse("A00001", "서울만남(부산)휴게소", "경부선", null)));

        mockMvc.perform(get("/api/admin/rest-stops/oil-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].unitName").value("서울만남(부산)휴게소"));
    }

    @Test
    @DisplayName("GET /api/admin/oil-stations/search는 이름으로 주유소를 검색한다")
    void search_returnsOk() throws Exception {
        when(adminRestOilLinkService.search("마장"))
                .thenReturn(List.of(new AdminOilStationSearchResponse(1L, "SK에너지 마장주유소", "중부내륙선", null)));

        mockMvc.perform(get("/api/admin/oil-stations/search").param("name", "마장"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].standardRestName").value("SK에너지 마장주유소"));
    }

    @Test
    @DisplayName("PUT /api/admin/oil-stations/{oilId}/link는 연결하고 활동 로그를 남긴다")
    void link_returnsOkAndLogs() throws Exception {
        when(adminRestOilLinkService.link(1L, "A00099"))
                .thenReturn(new AdminOilStationLinkResponse(1L, "SK에너지 마장주유소", "A00099", "마장휴게소", true));

        mockMvc.perform(put("/api/admin/oil-stations/1/link")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminOilStationLinkRequest("A00099"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restStopServiceAreaCode").value("A00099"));

        verify(adminActivityLogService).logOilStationLinked(authentication, "SK에너지 마장주유소", "마장휴게소");
    }

    @Test
    @DisplayName("PUT /api/admin/oil-stations/{oilId}/link는 휴게소가 없으면 404를 반환한다")
    void link_returnsNotFoundWhenRestStopMissing() throws Exception {
        doThrow(RestStopNotFoundException.forServiceAreaCode("UNKNOWN"))
                .when(adminRestOilLinkService)
                .link(1L, "UNKNOWN");

        mockMvc.perform(put("/api/admin/oil-stations/1/link")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminOilStationLinkRequest("UNKNOWN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/admin/oil-stations/{oilId}/link는 주유소가 없으면 404를 반환한다")
    void link_returnsNotFoundWhenOilMissing() throws Exception {
        doThrow(RestOilNotFoundException.forId(99L))
                .when(adminRestOilLinkService)
                .link(99L, "A00001");

        mockMvc.perform(put("/api/admin/oil-stations/99/link")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminOilStationLinkRequest("A00001"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/admin/oil-stations/{oilId}/link는 연결을 해제하고 활동 로그를 남긴다")
    void unlink_returnsOkAndLogs() throws Exception {
        when(adminRestOilLinkService.unlink(1L))
                .thenReturn(new AdminOilStationLinkResponse(1L, "SK에너지 마장주유소", null, null, true));

        mockMvc.perform(delete("/api/admin/oil-stations/1/link").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restStopServiceAreaCode").doesNotExist());

        verify(adminActivityLogService).logOilStationUnlinked(authentication, "SK에너지 마장주유소");
    }

    @Test
    @DisplayName("DELETE /api/admin/oil-stations/{oilId}/override는 잠금을 해제하고 활동 로그를 남긴다")
    void clearOverride_returnsOkAndLogs() throws Exception {
        when(adminRestOilLinkService.clearOverride(1L))
                .thenReturn(new AdminOilStationLinkResponse(1L, "SK에너지 마장주유소", "A00099", "마장휴게소", false));

        mockMvc.perform(delete("/api/admin/oil-stations/1/override").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminOverridden").value(false));

        verify(adminActivityLogService).logOilStationOverrideCleared(authentication, "SK에너지 마장주유소");
    }
}
