package com.restroute.reststop.controller.admin;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.common.GlobalExceptionHandler;
import com.restroute.reststop.controller.request.AdminRestroomLinkRequest;
import com.restroute.reststop.controller.response.AdminRestStopRestroomLinkSummaryResponse;
import com.restroute.reststop.controller.response.AdminRestroomLinkResponse;
import com.restroute.reststop.controller.response.AdminRestroomSearchResponse;
import com.restroute.reststop.service.image.exception.RestStopNotFoundException;
import com.restroute.reststop.service.restroom.AdminRestStopRestroomLinkService;
import com.restroute.reststop.service.restroom.exception.RestStopRestroomNotFoundException;
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
class AdminRestStopRestroomLinkControllerTest {

    @Mock
    private AdminRestStopRestroomLinkService adminRestStopRestroomLinkService;

    @Mock
    private AdminActivityLogService adminActivityLogService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRestStopRestroomLinkController(
                        adminRestStopRestroomLinkService, adminActivityLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/rest-stops/restroom-links는 전체 휴게소별 연결 목록을 반환한다")
    void findAll_returnsOk() throws Exception {
        when(adminRestStopRestroomLinkService.findAll())
                .thenReturn(List.of(new AdminRestStopRestroomLinkSummaryResponse("A00001", "죽전(서울)휴게소", "경부선", null)));

        mockMvc.perform(get("/api/admin/rest-stops/restroom-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].unitName").value("죽전(서울)휴게소"));
    }

    @Test
    @DisplayName("GET /api/admin/rest-stop-restrooms/search는 이름과 노선으로 화장실 현황을 검색한다")
    void search_returnsOk() throws Exception {
        when(adminRestStopRestroomLinkService.search("죽전", "경부선"))
                .thenReturn(List.of(new AdminRestroomSearchResponse(1L, "죽전(서울)", "경부선", null)));

        mockMvc.perform(get("/api/admin/rest-stop-restrooms/search")
                        .param("name", "죽전")
                        .param("routeName", "경부선"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sourceRestStopName").value("죽전(서울)"));
    }

    @Test
    @DisplayName("PUT /api/admin/rest-stop-restrooms/{restroomId}/link는 연결하고 활동 로그를 남긴다")
    void link_returnsOkAndLogs() throws Exception {
        when(adminRestStopRestroomLinkService.link(1L, "A00001"))
                .thenReturn(new AdminRestroomLinkResponse(1L, "죽전(서울)", "A00001", "죽전(서울)휴게소"));

        mockMvc.perform(put("/api/admin/rest-stop-restrooms/1/link")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminRestroomLinkRequest("A00001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restStopServiceAreaCode").value("A00001"));

        verify(adminActivityLogService).log(authentication, "죽전(서울) 화장실 현황을 죽전(서울)휴게소에 연결했습니다.");
    }

    @Test
    @DisplayName("PUT /api/admin/rest-stop-restrooms/{restroomId}/link는 휴게소가 없으면 404를 반환한다")
    void link_returnsNotFoundWhenRestStopMissing() throws Exception {
        doThrow(RestStopNotFoundException.forServiceAreaCode("UNKNOWN"))
                .when(adminRestStopRestroomLinkService)
                .link(1L, "UNKNOWN");

        mockMvc.perform(put("/api/admin/rest-stop-restrooms/1/link")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminRestroomLinkRequest("UNKNOWN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/admin/rest-stop-restrooms/{restroomId}/link는 화장실 현황이 없으면 404를 반환한다")
    void link_returnsNotFoundWhenRestroomMissing() throws Exception {
        doThrow(RestStopRestroomNotFoundException.forId(99L))
                .when(adminRestStopRestroomLinkService)
                .link(99L, "A00001");

        mockMvc.perform(put("/api/admin/rest-stop-restrooms/99/link")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminRestroomLinkRequest("A00001"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/admin/rest-stop-restrooms/{restroomId}/link는 연결을 해제하고 활동 로그를 남긴다")
    void unlink_returnsOkAndLogs() throws Exception {
        when(adminRestStopRestroomLinkService.unlink(1L))
                .thenReturn(new AdminRestroomLinkResponse(1L, "죽전(서울)", "", null));

        mockMvc.perform(delete("/api/admin/rest-stop-restrooms/1/link").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restStopServiceAreaCode").value(""));

        verify(adminActivityLogService).log(authentication, "죽전(서울) 화장실 현황의 연결을 해제했습니다.");
    }
}
