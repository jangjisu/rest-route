package com.restroute.oilprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.oilprice.client.OpinetApiClient;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceItem;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceResponse;
import com.restroute.oilprice.domain.NationalOilPriceEntity;
import com.restroute.oilprice.repository.NationalOilPriceRepository;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class NationalOilPriceServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-07T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 7);

    @Mock
    private OpinetApiClient opinetApiClient;

    @Mock
    private NationalOilPriceRepository nationalOilPriceRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private NationalOilPriceService service;

    @BeforeEach
    void setUp() {
        service = new NationalOilPriceService(opinetApiClient, nationalOilPriceRepository, transactionTemplate, CLOCK);
    }

    @Test
    @DisplayName("오늘 평균가가 DB에 있으면 오피넷을 호출하지 않고 요약을 반환한다")
    void getTodaySummary_usesStoredRows() throws Exception {
        when(nationalOilPriceRepository.findAllByTradeDate(TODAY))
                .thenReturn(List.of(
                        entity("B027", "휘발유", "1892.88", "-4.19"),
                        entity("D047", "자동차용경유", "1880.08", "-4.51"),
                        entity("K015", "자동차용부탄", "1135.19", "+0.01")));

        Optional<NationalOilPriceSummary> result = service.getTodaySummary();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().tradeDate()).isEqualTo("2026.07.07");
        assertThat(result.orElseThrow().gasoline().price()).isEqualTo("1,893원");
        assertThat(result.orElseThrow().diesel().price()).isEqualTo("1,880원");
        assertThat(result.orElseThrow().lpg().price()).isEqualTo("1,135원");
        verify(opinetApiClient, never()).getAverageOilPrices();
    }

    @Test
    @DisplayName("오늘 평균가가 DB에 없으면 오피넷을 호출해 저장한 뒤 요약을 반환한다")
    void getTodaySummary_fetchesAndStoresWhenMissing() throws Exception {
        NationalOilPriceEntity gasoline = entity("B027", "휘발유", "1892.88", "-4.19");
        NationalOilPriceEntity diesel = entity("D047", "자동차용경유", "1880.08", "-4.51");
        NationalOilPriceEntity lpg = entity("K015", "자동차용부탄", "1135.19", "+0.01");
        when(nationalOilPriceRepository.findAllByTradeDate(TODAY))
                .thenReturn(List.of())
                .thenReturn(List.of(gasoline, diesel, lpg));
        when(opinetApiClient.getAverageOilPrices()).thenReturn(response(gasoline, diesel, lpg));
        when(transactionTemplate.execute(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation
                        .<org.springframework.transaction.support.TransactionCallback<Integer>>getArgument(0)
                        .doInTransaction(null));

        Optional<NationalOilPriceSummary> result = service.getTodaySummary();

        assertThat(result).isPresent();
        verify(nationalOilPriceRepository).deleteAllByTradeDate(TODAY);
        verify(nationalOilPriceRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("오피넷 응답이 비어 있으면 저장하지 않고 평균가 요약을 생략한다")
    void getTodaySummary_returnsEmptyWhenFetchReturnsNoItems() {
        when(nationalOilPriceRepository.findAllByTradeDate(TODAY))
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(opinetApiClient.getAverageOilPrices())
                .thenReturn(new OpinetAverageOilPriceResponse(new OpinetAverageOilPriceResponse.Result(List.of())));
        when(transactionTemplate.execute(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation
                        .<org.springframework.transaction.support.TransactionCallback<Integer>>getArgument(0)
                        .doInTransaction(null));

        Optional<NationalOilPriceSummary> result = service.getTodaySummary();

        assertThat(result).isEmpty();
        verify(nationalOilPriceRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("필수 유종 평균가가 누락되면 평균가 요약을 생략한다")
    void getTodaySummary_returnsEmptyWhenRequiredProductIsMissing() throws Exception {
        NationalOilPriceEntity gasoline = entity("B027", "휘발유", "1892.88", "-4.19");
        when(nationalOilPriceRepository.findAllByTradeDate(TODAY))
                .thenReturn(List.of())
                .thenReturn(List.of(gasoline));
        when(opinetApiClient.getAverageOilPrices()).thenReturn(response(gasoline));
        when(transactionTemplate.execute(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation
                        .<org.springframework.transaction.support.TransactionCallback<Integer>>getArgument(0)
                        .doInTransaction(null));

        Optional<NationalOilPriceSummary> result = service.getTodaySummary();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("LPG 평균가만 누락되어도 평균가 요약을 생략한다")
    void getTodaySummary_returnsEmptyWhenLpgIsMissing() throws Exception {
        NationalOilPriceEntity gasoline = entity("B027", "휘발유", "1892.88", "-4.19");
        NationalOilPriceEntity diesel = entity("D047", "자동차용경유", "1880.08", "-4.51");
        when(nationalOilPriceRepository.findAllByTradeDate(TODAY)).thenReturn(List.of(gasoline, diesel));

        Optional<NationalOilPriceSummary> result = service.getTodaySummary();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("같은 제품 코드가 중복 저장되어도 첫 번째 평균가로 요약한다")
    void getTodaySummary_usesFirstRowWhenProductCodeIsDuplicated() throws Exception {
        NationalOilPriceEntity gasoline = entity("B027", "휘발유", "1892.88", "-4.19");
        NationalOilPriceEntity duplicatedGasoline = entity("B027", "휘발유", "1900.00", "-1.00");
        NationalOilPriceEntity diesel = entity("D047", "자동차용경유", "1880.08", "-4.51");
        NationalOilPriceEntity lpg = entity("K015", "자동차용부탄", "1135.19", "+0.01");
        when(nationalOilPriceRepository.findAllByTradeDate(TODAY))
                .thenReturn(List.of(gasoline, duplicatedGasoline, diesel, lpg));

        Optional<NationalOilPriceSummary> result = service.getTodaySummary();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().gasoline().price()).isEqualTo("1,893원");
    }

    @Test
    @DisplayName("오피넷 호출이 실패하면 평균가 요약을 생략한다")
    void getTodaySummary_returnsEmptyWhenFetchFails() {
        when(nationalOilPriceRepository.findAllByTradeDate(TODAY)).thenReturn(List.of());
        when(opinetApiClient.getAverageOilPrices()).thenThrow(new IllegalStateException("opinet down"));

        Optional<NationalOilPriceSummary> result = service.getTodaySummary();

        assertThat(result).isEmpty();
    }

    private OpinetAverageOilPriceResponse response(NationalOilPriceEntity... entities) throws Exception {
        List<OpinetAverageOilPriceItem> items = java.util.Arrays.stream(entities)
                .map(entity ->
                        item(entity.getProductCode(), entity.getProductName(), entity.getPrice(), entity.getDiff()))
                .toList();
        return new OpinetAverageOilPriceResponse(new OpinetAverageOilPriceResponse.Result(items));
    }

    private NationalOilPriceEntity entity(String productCode, String productName, String price, String diff)
            throws Exception {
        return NationalOilPriceEntity.from(item(productCode, productName, price, diff));
    }

    private OpinetAverageOilPriceItem item(String productCode, String productName, String price, String diff) {
        try {
            return new ObjectMapper()
                    .readValue("""
                            {
                              "TRADE_DT": "20260707",
                              "PRODCD": "%s",
                              "PRODNM": "%s",
                              "PRICE": "%s",
                              "DIFF": "%s"
                            }
                            """.formatted(productCode, productName, price, diff), OpinetAverageOilPriceItem.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
