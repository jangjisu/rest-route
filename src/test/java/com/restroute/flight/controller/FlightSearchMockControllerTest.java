package com.restroute.flight.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FlightSearchMockControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightSearchMockController())
                .build();
    }

    @Test
    @DisplayName("파라미터 없이 호출하면 기본 20건과 전체 개수/다음 커서를 반환한다")
    void searchMock_returnsFirstPageWithDefaultSize() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.meta.totalCount").value(342))
                .andExpect(jsonPath("$.data.meta.hasNext").value(true))
                .andExpect(jsonPath("$.data.meta.nextCursor").value("deal_00019"))
                .andExpect(jsonPath("$.data.items[0].id").value("deal_00000"))
                .andExpect(jsonPath("$.data.items[0].destination.iata").value("FUK"))
                .andExpect(jsonPath("$.data.items[0].destination.city").value("후쿠오카"))
                .andExpect(jsonPath("$.data.items[0].price.currency").value("KRW"));
    }

    @Test
    @DisplayName("이전 응답의 nextCursor를 넘기면 그다음부터 이어서 반환한다")
    void searchMock_continuesFromGivenCursor() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock").param("cursor", "deal_00019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("deal_00020"))
                .andExpect(jsonPath("$.data.items.length()").value(20));
    }

    @Test
    @DisplayName("size 파라미터로 페이지 크기를 조절할 수 있다")
    void searchMock_respectsRequestedSize() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.meta.nextCursor").value("deal_00004"));
    }

    @Test
    @DisplayName("size가 최대치를 넘으면 50으로 잘린다")
    void searchMock_capsSizeAtMax() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock").param("size", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(50));
    }

    @Test
    @DisplayName("size가 0 이하이면 최소 1로 보정된다")
    void searchMock_flooresSizeAtOne() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock").param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    @DisplayName("마지막 페이지에서는 hasNext가 false이고 nextCursor가 없다")
    void searchMock_lastPageHasNoNextCursor() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("cursor", "deal_00335")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(6))
                .andExpect(jsonPath("$.data.meta.hasNext").value(false))
                .andExpect(jsonPath("$.data.meta.nextCursor").value(nullValue()));
    }

    @Test
    @DisplayName("알 수 없는 커서가 오면 처음부터 반환한다")
    void searchMock_fallsBackToStartForUnknownCursor() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock").param("cursor", "not-a-real-cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("deal_00000"));
    }

    @Test
    @DisplayName("deal_ 접두사인데 숫자가 아닌 커서도 처음부터 반환한다")
    void searchMock_fallsBackToStartForNonNumericCursorSuffix() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock").param("cursor", "deal_abcde"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("deal_00000"));
    }
}
