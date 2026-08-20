package com.restroute.flight.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.flight.service.FlightSearchMockService;
import com.restroute.holiday.domain.HolidayEntity;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    private static final int MOCK_TOTAL_SIZE = 77;
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private static final DateTimeFormatter LEG_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final String VALID_ORIGIN = "ICN";
    private static final LocalDate VALID_DATE_FROM_DATE = LocalDate.now().plusDays(10);
    private static final String VALID_DATE_FROM = VALID_DATE_FROM_DATE.toString();
    private static final String VALID_DATE_TO =
            VALID_DATE_FROM_DATE.plusDays(31).toString();

    /** 첫 항목(index 0)의 예상 출/도착 시각 — {@link com.restroute.flight.service.FlightSearchMockFixture}의
     * 계산식(09:20 출발, 90분 소요, 3박 뒤 13:10 귀국)을 그대로 재현한다. */
    private static final String EXPECTED_DEPARTURE_DEPART_AT =
            LEG_TIME_FORMAT.format(VALID_DATE_FROM_DATE.atTime(9, 20).atOffset(KST));

    private static final String EXPECTED_DEPARTURE_ARRIVE_AT = LEG_TIME_FORMAT.format(
            VALID_DATE_FROM_DATE.atTime(9, 20).atOffset(KST).plusMinutes(90));
    private static final String EXPECTED_ARRIVAL_DEPART_AT = LEG_TIME_FORMAT.format(
            VALID_DATE_FROM_DATE.plusDays(3).atTime(13, 10).atOffset(KST));
    private static final String EXPECTED_ARRIVAL_ARRIVE_AT = LEG_TIME_FORMAT.format(
            VALID_DATE_FROM_DATE.plusDays(3).atTime(13, 10).atOffset(KST).plusMinutes(90));

    /** 첫 항목의 출발일~귀국일(3박) 사이 주말 수 — includeWeekend=true라 걸러지지 않고 holidays에 그대로 잡힌다. */
    private static final int EXPECTED_WEEKEND_HOLIDAY_COUNT =
            countWeekendDays(VALID_DATE_FROM_DATE, VALID_DATE_FROM_DATE.plusDays(3));

    private static int countWeekendDays(LocalDate from, LocalDate to) {
        int count = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (HolidayEntity.isWeekend(date)) {
                count++;
            }
        }
        return count;
    }

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightSearchMockController(new FlightSearchMockService()))
                .setControllerAdvice(new FlightExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("유효한 요청이면 첫 항목이 계산된 값 그대로 채워진 첫 페이지를 반환한다")
    void searchMock_returnsFirstPageWithComputedFields() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("includeWeekend", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.length()").value(20))
                .andExpect(jsonPath("$.meta.totalCount").value(MOCK_TOTAL_SIZE))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andExpect(jsonPath("$.meta.locale").value("ko"))
                .andExpect(jsonPath("$.meta.currency").value("krw"))
                .andExpect(jsonPath("$.meta.fetchedAt").exists())
                .andExpect(jsonPath("$.data[0].id").value(matchesPattern("^[A-Za-z0-9]{4}_0001$")))
                .andExpect(jsonPath("$.data[0].destination.code").value("FUK"))
                .andExpect(jsonPath("$.data[0].destination.name").value("후쿠오카"))
                .andExpect(jsonPath("$.data[0].departure.departAt").value(EXPECTED_DEPARTURE_DEPART_AT))
                .andExpect(jsonPath("$.data[0].departure.arriveAt").value(EXPECTED_DEPARTURE_ARRIVE_AT))
                .andExpect(jsonPath("$.data[0].departure.duration").value(90))
                .andExpect(jsonPath("$.data[0].departure.transferCount").value(1))
                .andExpect(jsonPath("$.data[0].arrival.departAt").value(EXPECTED_ARRIVAL_DEPART_AT))
                .andExpect(jsonPath("$.data[0].arrival.arriveAt").value(EXPECTED_ARRIVAL_ARRIVE_AT))
                .andExpect(jsonPath("$.data[0].arrival.duration").value(90))
                .andExpect(jsonPath("$.data[0].arrival.transferCount").value(1))
                .andExpect(jsonPath("$.data[0].nights").value(3))
                .andExpect(jsonPath("$.data[0].holidays.length()").value(EXPECTED_WEEKEND_HOLIDAY_COUNT))
                .andExpect(jsonPath("$.data[0].airline.code").value("LJ"))
                .andExpect(jsonPath("$.data[0].airline.name").value("진에어"))
                .andExpect(jsonPath("$.data[0].airline.isLowCost").value(true))
                .andExpect(jsonPath("$.data[0].price.amount").value(89000))
                .andExpect(jsonPath("$.data[0].price.currency").value("KRW"))
                .andExpect(jsonPath("$.data[0].isLowestInRange").value(true))
                .andExpect(jsonPath("$.data[0].gateName").value("Aviasales"))
                .andExpect(jsonPath("$.data[0].seatsLeft").value(nullValue()));
    }

    @Test
    @DisplayName("가격 기준 전체 최저가는 정확히 한 건에만 isLowestInRange=true가 붙는다")
    void searchMock_marksExactlyOneItemAsLowestInRange() throws Exception {
        String body = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("limit", String.valueOf(MOCK_TOTAL_SIZE)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode items = objectMapper.readTree(body).at("/data");
        int lowestCount = 0;
        int minPrice = Integer.MAX_VALUE;
        for (JsonNode item : items) {
            minPrice = Math.min(minPrice, item.at("/price/amount").asInt());
            if (item.at("/isLowestInRange").asBoolean()) {
                lowestCount++;
            }
        }

        assertThat(lowestCount).isEqualTo(1);
        int finalMinPrice = minPrice;
        assertThat(items.get(0).at("/price/amount").asInt()).isEqualTo(finalMinPrice);
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
                .andExpect(jsonPath("$.data[0].departure.transferCount").value(1));
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
                .andExpect(jsonPath("$.data[2].departure.transferCount").value(0))
                .andExpect(jsonPath("$.data[3].arrival.transferCount").value(0));
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
                        .param("limit", "30"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode items = objectMapper.readTree(body).at("/data");
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
                        .param("limit", "30"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode items = objectMapper.readTree(body).at("/data");
        List<String> departures = new ArrayList<>();
        for (JsonNode item : items) {
            departures.add(item.at("/departure/departAt").asText());
        }

        assertThat(departures).isSorted();
    }

    @Test
    @DisplayName("sort가 PRICE/DATE가 아니면 validation_failed로 invalid_sort를 반환한다")
    void searchMock_returnsValidationFailedForInvalidSort() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("sort", "CHEAPEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"))
                .andExpect(jsonPath("$.error.details[0].field").value("sort"))
                .andExpect(jsonPath("$.error.details[0].code").value("invalid_sort"));
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
                .andExpect(jsonPath("$.data[0].destination.code").value("OSA"))
                .andExpect(jsonPath("$.data[4].destination.code").value("OSA"));
    }

    @Test
    @DisplayName("필수 파라미터가 여러 개 없으면 DTO 생성자가 한 번에 모아서 validation_failed로 반환한다")
    void searchMock_returnsValidationFailedForMissingRequiredParams() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("validation_failed"))
                .andExpect(jsonPath("$.error.details.length()").value(4))
                .andExpect(jsonPath("$.error.details[0].field").value("origin"))
                .andExpect(jsonPath("$.error.details[0].code").value("required"));
    }

    @Test
    @DisplayName("searchMode가 없으면 required로 실패한다")
    void searchMock_returnsValidationFailedWhenSearchModeMissing() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("searchMode"))
                .andExpect(jsonPath("$.error.details[0].code").value("required"));
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
                .andExpect(jsonPath("$.error.details[0].code").value("nights_not_allowed_in_fixed_mode"));
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
                .andExpect(jsonPath("$.error.details[0].code").value("sector_destination_conflict"));
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
                .andExpect(jsonPath("$.error.details[0].code").value("invalid_currency"));
    }

    @Test
    @DisplayName("nights를 생략하면 dateFrom~dateTo 기간(1~31박) 전체를 대상으로 순회하며 정상 응답한다")
    void searchMock_defaultsToDateRangeNightsWhenOmitted() throws Exception {
        assertThat(allNightsValuesOf(VALID_DATE_TO)).isEqualTo(rangeSet(1, 31));
    }

    @Test
    @DisplayName("nights를 생략했을 때 기간이 좁으면 그 기간만큼만 순회한다(고정 10이 아님)")
    void searchMock_cyclesWithinNarrowerDateRangeWhenNightsOmitted() throws Exception {
        assertThat(allNightsValuesOf(VALID_DATE_FROM_DATE.plusDays(5).toString()))
                .isEqualTo(rangeSet(1, 5));
    }

    /** limit은 최대 50으로 잘리므로(MOCK_TOTAL_SIZE=77), 전체 nights 값을 모으려면 페이지를 끝까지 순회해야 한다. */
    private Set<Integer> allNightsValuesOf(String dateTo) throws Exception {
        Set<Integer> nights = new HashSet<>();
        String cursor = null;
        boolean hasNext = true;

        while (hasNext) {
            var requestBuilder = get("/api/flights/search/mock")
                    .param("origin", VALID_ORIGIN)
                    .param("searchMode", "range")
                    .param("dateFrom", VALID_DATE_FROM)
                    .param("dateTo", dateTo)
                    .param("includeWeekend", "true")
                    .param("limit", "50");
            if (cursor != null) {
                requestBuilder = requestBuilder.param("cursor", cursor);
            }

            String body = mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode json = objectMapper.readTree(body);
            for (JsonNode item : json.at("/data")) {
                nights.add(item.at("/nights").asInt());
            }

            hasNext = json.at("/meta/hasNext").asBoolean();
            cursor = hasNext ? json.at("/meta/nextCursor").asText() : null;
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
    @DisplayName("필수 파라미터가 값은 왔지만 빈 문자열이면 DTO 생성자가 required로 잡는다")
    void searchMock_returnsValidationFailedForBlankRequiredParam() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", "")
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"))
                .andExpect(jsonPath("$.error.details.length()").value(1))
                .andExpect(jsonPath("$.error.details[0].field").value("origin"))
                .andExpect(jsonPath("$.error.details[0].code").value("required"));
    }

    @Test
    @DisplayName("형식이 이상한 커서(세션 토큰을 못 뽑음)는 deal_not_found 에러를 반환한다")
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
                .andExpect(jsonPath("$.error.code").value("deal_not_found"));
    }

    @Test
    @DisplayName("유효한 세션 안에 실제로 없는 id가 오면 deal_not_found 에러를 반환한다")
    void searchMock_returnsDealNotFoundForIdMissingWithinValidSession() throws Exception {
        String firstPageBody = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("limit", "1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String firstItemId =
                objectMapper.readTree(firstPageBody).at("/data/0/id").asText();
        String sessionToken = firstItemId.substring(0, firstItemId.indexOf('_'));

        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("cursor", sessionToken + "_9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("deal_not_found"))
                .andExpect(jsonPath("$.error.message").value("Deal not found or already expired"));
    }

    @Test
    @DisplayName("검색 조건이 달라진 채로 이전 cursor를 재사용하면 deal_not_found 에러를 반환한다")
    void searchMock_returnsDealNotFoundWhenSearchConditionsChange() throws Exception {
        String firstPageBody = mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("limit", "5"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String cursorFromDifferentConditions =
                objectMapper.readTree(firstPageBody).at("/meta/nextCursor").asText();

        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "4")
                        .param("cursor", cursorFromDifferentConditions)
                        .param("limit", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("deal_not_found"));
    }

    @Test
    @DisplayName("실제 nextCursor를 계속 이어가면 중복/누락 없이 77건 전체를 정확히 순회한다")
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
                    .param("includeWeekend", "true")
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
            JsonNode items = json.at("/data");
            for (JsonNode item : items) {
                assertThat(seenIds.add(item.get("id").asText())).isTrue();
            }

            lastPageSize = items.size();
            hasNext = json.at("/meta/hasNext").asBoolean();
            cursor = hasNext ? json.at("/meta/nextCursor").asText() : null;
            pageCount++;
        }

        assertThat(seenIds).hasSize(MOCK_TOTAL_SIZE);
        assertThat(pageCount).isEqualTo(4);
        assertThat(lastPageSize).isEqualTo(17);
    }
}
