package com.restroute.common.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HomeController 는 GET / 에서 HTML 껍데기만 서빙한다. 프론트엔드 JS가 각 섹션을 비동기로 채운다.
 * User-Agent로 모바일 기기를 감지해 모바일은 finder(모바일 전용 화면)로, 그 외(데스크톱, User-Agent
 * 없음)는 index(지도 화면)로 보낸다.
 */
@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    private static final String IPHONE_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko)"
                    + " Version/17.5 Mobile/15E148 Safari/604.1";
    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)"
                    + " Chrome/126.0.0.0 Safari/537.36";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("index_noUserAgent_returns200AndIndexView")
    void index_noUserAgent_returns200AndIndexView() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
    }

    @Test
    @DisplayName("index_desktopUserAgent_returnsIndexView")
    void index_desktopUserAgent_returnsIndexView() throws Exception {
        mockMvc.perform(get("/").header("User-Agent", DESKTOP_USER_AGENT))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("index_mobileUserAgent_returnsFinderView")
    void index_mobileUserAgent_returnsFinderView() throws Exception {
        mockMvc.perform(get("/").header("User-Agent", IPHONE_USER_AGENT))
                .andExpect(status().isOk())
                .andExpect(view().name("finder"));
    }

    @Test
    @DisplayName("index_modelHasNoDataAttributes")
    void index_modelHasNoDataAttributes() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(model().attributeDoesNotExist("summary"))
                .andExpect(model().attributeDoesNotExist("tollGates"))
                .andExpect(model().attributeDoesNotExist("hourlyPattern"));
    }

    @Test
    @DisplayName("finder_returns200AndFinderView")
    void finder_returns200AndFinderView() throws Exception {
        mockMvc.perform(get("/finder")).andExpect(status().isOk()).andExpect(view().name("finder"));
    }
}
