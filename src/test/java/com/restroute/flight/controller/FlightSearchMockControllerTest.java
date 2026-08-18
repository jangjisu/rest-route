package com.restroute.flight.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.flight.service.FlightSearchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FlightSearchMockControllerTest {

    private static final String VALID_ORIGIN = "ICN";
    private static final String VALID_DATE_FROM = "2099-01-10";
    private static final String VALID_DATE_TO = "2099-02-10";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightSearchMockController(new FlightSearchService()))
                .setControllerAdvice(new FlightExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("유효한 요청이면 첫 항목이 계산된 값 그대로 채워진 첫 페이지를 반환한다 (totalSize 기본값 77)")
    void searchMock_returnsFirstPageWithComputedFields() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.meta.totalCount").value(77))
                .andExpect(jsonPath("$.data.meta.hasNext").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(matchesPattern("^[A-Za-z0-9]{4}_0001$")))
                .andExpect(jsonPath("$.data.items[0].destination.code").value("FUK"))
                .andExpect(jsonPath("$.data.items[0].destination.name").value("후쿠오카"))
                .andExpect(jsonPath("$.data.items[0].departure.departureFrom").value("2099-01-10T09:20:00+09:00"))
                .andExpect(jsonPath("$.data.items[0].departure.departTo").value("2099-01-10T10:50:00+09:00"))
                .andExpect(jsonPath("$.data.items[0].departure.duration").value(90))
                .andExpect(jsonPath("$.data.items[0].departure.transferCount").value(1))
                .andExpect(jsonPath("$.data.items[0].arrival.departureFrom").value("2099-01-13T13:10:00+09:00"))
                .andExpect(jsonPath("$.data.items[0].arrival.departTo").value("2099-01-13T14:40:00+09:00"))
                .andExpect(jsonPath("$.data.items[0].arrival.duration").value(90))
                .andExpect(jsonPath("$.data.items[0].arrival.transferCount").value(1))
                .andExpect(jsonPath("$.data.items[0].nights").value(3))
                .andExpect(jsonPath("$.data.items[0].airline.code").value("LJ"))
                .andExpect(jsonPath("$.data.items[0].airline.name").value("진에어"))
                .andExpect(jsonPath("$.data.items[0].price.amount").value(89000))
                .andExpect(jsonPath("$.data.items[0].price.currency").value("KRW"))
                .andExpect(jsonPath("$.data.items[0].gateName").value("Trip.com"));
    }

    @Test
    @DisplayName("totalSize를 지정하면 그만큼만 생성된다")
    void searchMock_generatesExactlyTotalSizeItems() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("totalSize", "5")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.meta.totalCount").value(5))
                .andExpect(jsonPath("$.data.meta.hasNext").value(false));
    }

    @Test
    @DisplayName("totalSize가 범위를 벗어나면 컨트롤러가 조용히 클램프한다")
    void searchMock_clampsOutOfRangeTotalSize() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("totalSize", "5000")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meta.totalCount").value(1000));
    }

    @Test
    @DisplayName("includeTransfer가 없으면(기본 켜짐) 경유가 섞여 나온다")
    void searchMock_includesTransfersByDefaultWhenOmitted() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].departure.transferCount").value(1));
    }

    @Test
    @DisplayName("includeTransfer=false면 모든 항목이 직항(transferCount=0)이다")
    void searchMock_directOnlyWhenIncludeTransferFalse() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("includeTransfer", "false")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[2].departure.transferCount").value(0))
                .andExpect(jsonPath("$.data.items[3].arrival.transferCount").value(0));
    }

    @Test
    @DisplayName("sort가 없으면(기본 PRICE) 가격 오름차순으로 정렬된 결과를 반환한다")
    void searchMock_sortsByPriceAscendingByDefault() throws Exception {
        String body = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("totalSize", "30")
                        .param("limit", "30"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode items = objectMapper.readTree(body).at("/data/items");
        List<Integer> prices = new ArrayList<>();
        for (JsonNode item : items) {
            prices.add(item.at("/price/amount").asInt());
        }

        assertThat(prices).isSorted();
    }

    @Test
    @DisplayName("sort=DATE면 출발일 오름차순으로 정렬된 결과를 반환한다")
    void searchMock_sortsByDepartureDateAscending_whenSortIsDate() throws Exception {
        String body = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("sort", "DATE")
                        .param("totalSize", "30")
                        .param("limit", "30"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode items = objectMapper.readTree(body).at("/data/items");
        List<String> departures = new ArrayList<>();
        for (JsonNode item : items) {
            departures.add(item.at("/departure/departureFrom").asText());
        }

        assertThat(departures).isSorted();
    }

    @Test
    @DisplayName("sort가 PRICE/DATE가 아니면 VALIDATION_FAILED로 INVALID_SORT를 반환한다")
    void searchMock_returnsValidationFailedForInvalidSort() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("sort", "CHEAPEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("sort"))
                .andExpect(jsonPath("$.error.details[0].code").value("INVALID_SORT"));
    }

    @Test
    @DisplayName("destination을 지정하면 모든 항목이 그 목적지로 고정된다")
    void searchMock_fixesDestinationWhenGiven() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("destination", "OSA")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].destination.code").value("OSA"))
                .andExpect(jsonPath("$.data.items[4].destination.code").value("OSA"));
    }

    @Test
    @DisplayName("필수 파라미터가 여러 개 없으면 DTO 생성자가 한 번에 모아서 VALIDATION_FAILED로 반환한다")
    void searchMock_returnsValidationFailedForMissingRequiredParams() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details.length()").value(4))
                .andExpect(jsonPath("$.error.details[0].field").value("origin"))
                .andExpect(jsonPath("$.error.details[0].code").value("REQUIRED"));
    }

    @Test
    @DisplayName("searchMode가 없으면 REQUIRED로 실패한다")
    void searchMock_returnsValidationFailedWhenSearchModeMissing() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("searchMode"))
                .andExpect(jsonPath("$.error.details[0].code").value("REQUIRED"));
    }

    @Test
    @DisplayName("지정날짜(fixed)에 nights를 보내면 실패한다")
    void searchMock_returnsValidationFailedWhenNightsSentInFixedMode() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "fixed")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("nights"))
                .andExpect(jsonPath("$.error.details[0].code").value("NIGHTS_NOT_ALLOWED_IN_FIXED_MODE"));
    }

    @Test
    @DisplayName("destination과 sector를 함께 보내면 실패한다")
    void searchMock_returnsValidationFailedWhenSectorCombinedWithDestination() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("destination", "OSA")
                        .param("sector", "JAPAN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("sector"))
                .andExpect(jsonPath("$.error.details[0].code").value("SECTOR_DESTINATION_CONFLICT"));
    }

    @Test
    @DisplayName("currency가 krw가 아니면 실패한다")
    void searchMock_returnsValidationFailedWhenCurrencyNotKrw() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("currency", "usd"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("currency"))
                .andExpect(jsonPath("$.error.details[0].code").value("INVALID_CURRENCY"));
    }

    @Test
    @DisplayName("nights를 생략하면 dateFrom~dateTo 기간(1~31박) 전체를 대상으로 순회하며 정상 응답한다")
    void searchMock_defaultsToDateRangeNightsWhenOmitted() throws Exception {
        String body = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("totalSize", "31")
                        .param("limit", "31"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(nightsValuesOf(body)).isEqualTo(rangeSet(1, 31));
    }

    @Test
    @DisplayName("nights를 생략했을 때 기간이 좁으면 그 기간만큼만 순회한다(고정 10이 아님)")
    void searchMock_cyclesWithinNarrowerDateRangeWhenNightsOmitted() throws Exception {
        String body = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", "2099-01-15")
                        .param("totalSize", "10")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(nightsValuesOf(body)).isEqualTo(rangeSet(1, 5));
    }

    private Set<Integer> nightsValuesOf(String responseBody) throws Exception {
        Set<Integer> nights = new HashSet<>();
        for (JsonNode item : objectMapper.readTree(responseBody).at("/data/items")) {
            nights.add(item.at("/nights").asInt());
        }
        return nights;
    }

    private static Set<Integer> rangeSet(int startInclusive, int endInclusive) {
        Set<Integer> range = new HashSet<>();
        for (int i = startInclusive; i <= endInclusive; i++) {
            range.add(i);
        }
        return range;
    }

    @Test
    @DisplayName("필수 파라미터가 값은 왔지만 빈 문자열이면 DTO 생성자가 REQUIRED로 잡는다")
    void searchMock_returnsValidationFailedForBlankRequiredParam() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", "")
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details.length()").value(1))
                .andExpect(jsonPath("$.error.details[0].field").value("origin"))
                .andExpect(jsonPath("$.error.details[0].code").value("REQUIRED"));
    }

    @Test
    @DisplayName("형식이 이상한 커서(세션 토큰을 못 뽑음)는 DEAL_NOT_FOUND 에러를 반환한다")
    void searchMock_returnsDealNotFoundForMalformedCursor() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("cursor", "not-a-real-cursor")
                        .param("limit", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEAL_NOT_FOUND"));
    }

    @Test
    @DisplayName("유효한 세션 안에 실제로 없는 id가 오면 DEAL_NOT_FOUND 에러를 반환한다")
    void searchMock_returnsDealNotFoundForIdMissingWithinValidSession() throws Exception {
        String firstPageBody = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("totalSize", "5")
                        .param("limit", "1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String firstItemId =
                objectMapper.readTree(firstPageBody).at("/data/items/0/id").asText();
        String sessionToken = firstItemId.substring(0, firstItemId.indexOf('_'));

        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("totalSize", "5")
                        .param("cursor", sessionToken + "_9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("DEAL_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Deal not found or already expired"));
    }

    @Test
    @DisplayName("검색 조건이 달라진 채로 이전 cursor를 재사용하면 DEAL_NOT_FOUND 에러를 반환한다")
    void searchMock_returnsDealNotFoundWhenSearchConditionsChange() throws Exception {
        String firstPageBody = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("totalSize", "10")
                        .param("limit", "5"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String cursorFromDifferentConditions =
                objectMapper.readTree(firstPageBody).at("/data/meta/nextCursor").asText();

        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "4")
                        .param("totalSize", "10")
                        .param("cursor", cursorFromDifferentConditions)
                        .param("limit", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEAL_NOT_FOUND"));
    }

    @Test
    @DisplayName("실제 nextCursor를 계속 이어가면 중복/누락 없이 지정한 totalSize 전체를 정확히 순회한다")
    void searchMock_paginatesThroughFullDatasetWithoutDuplicates() throws Exception {
        Set<String> seenIds = new HashSet<>();
        String cursor = null;
        boolean hasNext = true;
        int pageCount = 0;
        int lastPageSize = 0;

        while (hasNext) {
            var requestBuilder = get("/api/flights/search/mock")
                    .param("origin", VALID_ORIGIN)
                    .param("searchMode", "range")
                    .param("dateFrom", VALID_DATE_FROM)
                    .param("dateTo", VALID_DATE_TO)
                    .param("nights", "3")
                    .param("totalSize", "42")
                    .param("limit", "20");
            if (cursor != null) {
                requestBuilder = requestBuilder.param("cursor", cursor);
            }

            String body = mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode json = objectMapper.readTree(body);
            JsonNode items = json.at("/data/items");
            for (JsonNode item : items) {
                assertThat(seenIds.add(item.get("id").asText())).isTrue();
            }

            lastPageSize = items.size();
            hasNext = json.at("/data/meta/hasNext").asBoolean();
            cursor = hasNext ? json.at("/data/meta/nextCursor").asText() : null;
            pageCount++;
        }

        assertThat(seenIds).hasSize(42);
        assertThat(pageCount).isEqualTo(3);
        assertThat(lastPageSize).isEqualTo(2);
    }
}
