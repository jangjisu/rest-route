package com.restroute.controller.admin;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.common.GlobalExceptionHandler;
import com.restroute.controller.request.AdminFlightHolidayRequest;
import com.restroute.controller.response.AdminFlightHolidayResponse;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admin.AdminFlightHolidayService;
import com.restroute.service.admin.exception.DuplicateFlightHolidayException;
import com.restroute.service.admin.exception.FlightHolidayNotFoundException;
import java.time.LocalDate;
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
class AdminFlightHolidayControllerTest {

    @Mock
    private AdminFlightHolidayService adminFlightHolidayService;

    @Mock
    private AdminActivityLogService adminActivityLogService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminFlightHolidayController(adminFlightHolidayService, adminActivityLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AdminFlightHolidayRequest request() {
        return new AdminFlightHolidayRequest("2026-09-26", "대체공휴일");
    }

    private AdminFlightHolidayResponse response() {
        return new AdminFlightHolidayResponse(1L, "2026-09-26", "대체공휴일");
    }

    @Test
    @DisplayName("GET .../holidays는 전체 공휴일 목록을 반환한다")
    void findAll_returnsOk() throws Exception {
        when(adminFlightHolidayService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/admin/flights/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].date").value("2026-09-26"))
                .andExpect(jsonPath("$.data[0].name").value("대체공휴일"));
    }

    @Test
    @DisplayName("POST .../holidays는 공휴일을 추가하고 활동 로그를 남긴다")
    void create_returnsOkAndLogs() throws Exception {
        when(adminFlightHolidayService.create(request())).thenReturn(response());

        mockMvc.perform(post("/api/admin/flights/holidays")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("대체공휴일"));

        verify(adminActivityLogService).logFlightHolidayAdded(authentication, "2026-09-26", "대체공휴일");
    }

    @Test
    @DisplayName("POST .../holidays는 이미 등록된 날짜면 400을 반환한다")
    void create_returnsBadRequestWhenDuplicateDate() throws Exception {
        doThrow(DuplicateFlightHolidayException.forDate(LocalDate.of(2026, 9, 26)))
                .when(adminFlightHolidayService)
                .create(request());

        mockMvc.perform(post("/api/admin/flights/holidays")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("DELETE .../holidays/{id}는 공휴일을 삭제하고 활동 로그를 남긴다")
    void delete_returnsNoContentAndLogs() throws Exception {
        when(adminFlightHolidayService.delete(1L)).thenReturn(response());

        mockMvc.perform(delete("/api/admin/flights/holidays/1").principal(authentication))
                .andExpect(status().isNoContent());

        verify(adminActivityLogService).logFlightHolidayDeleted(authentication, "2026-09-26");
    }

    @Test
    @DisplayName("DELETE .../holidays/{id}는 존재하지 않으면 404를 반환한다")
    void delete_returnsNotFoundWhenMissing() throws Exception {
        doThrow(FlightHolidayNotFoundException.forId(99L))
                .when(adminFlightHolidayService)
                .delete(99L);

        mockMvc.perform(delete("/api/admin/flights/holidays/99").principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
