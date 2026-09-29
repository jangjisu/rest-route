package com.restroute.oilprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.config.TimeConfiguration;
import com.restroute.oilprice.client.OpinetApiClient;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceItem;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceResponse;
import com.restroute.oilprice.dto.NationalOilPriceSummary;
import com.restroute.oilprice.repository.NationalOilPriceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 호출자가 readOnly 트랜잭션을 연 상태(예: {@code RestStopNearbyQueryService.findNearby})에서
 * 오늘 평균가를 저장하는 경로의 회귀 테스트. H2는 read-only 연결에서의 쓰기를 거부하지 않아서
 * 운영 MySQL 에러 자체는 재현하지 못하고, 트랜잭션 전파 구조만 검증한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({NationalOilPriceService.class, TimeConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NationalOilPriceServiceTransactionTest {

    @Autowired
    private NationalOilPriceService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Clock clock;

    @MockitoBean
    private OpinetApiClient opinetApiClient;

    @MockitoSpyBean
    private NationalOilPriceRepository nationalOilPriceRepository;

    private TransactionTemplate readOnlyCaller;

    @BeforeEach
    void setUp() {
        readOnlyCaller = new TransactionTemplate(transactionManager);
        readOnlyCaller.setReadOnly(true);
        when(opinetApiClient.getAverageOilPrices()).thenReturn(todayResponse());
    }

    @AfterEach
    void tearDown() {
        nationalOilPriceRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("readOnly 트랜잭션 안에서 호출돼도 오늘 평균가는 쓰기 트랜잭션에서 저장·커밋된다")
    void getTodaySummary_savesOutsideCallerReadOnlyTransaction() {
        // 리포지토리 프록시 spy는 callRealMethod를 못 하므로, 저장 트랜잭션에서 saveAll 직전에 불리는
        // delete(빈 테이블이라 생략해도 무방)에서 readOnly 여부를 기록한다.
        AtomicReference<Boolean> savedInReadOnly = new AtomicReference<>();
        doAnswer(invocation -> {
                    savedInReadOnly.set(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
                    return null;
                })
                .when(nationalOilPriceRepository)
                .deleteAllByTradeDate(any());

        Optional<NationalOilPriceSummary> result = readOnlyCaller.execute(status -> service.getTodaySummary());

        assertThat(result).isPresent();
        assertThat(savedInReadOnly.get()).isFalse();
        assertThat(nationalOilPriceRepository.findAllByTradeDate(LocalDate.now(clock)))
                .hasSize(3);
    }

    @Test
    @DisplayName("저장이 실패해도 호출자 트랜잭션을 rollback-only로 만들지 않아 커밋이 성공한다")
    void getTodaySummary_doesNotMarkCallerRollbackOnlyWhenSaveFails() {
        doThrow(new IllegalStateException("save failed"))
                .when(nationalOilPriceRepository)
                .saveAll(anyList());

        assertThatCode(() -> readOnlyCaller.execute(status -> service.getTodaySummary()))
                .doesNotThrowAnyException();
    }

    private OpinetAverageOilPriceResponse todayResponse() {
        String today = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE);
        return new OpinetAverageOilPriceResponse(new OpinetAverageOilPriceResponse.Result(List.of(
                item(today, "B027", "휘발유", "1892.88"),
                item(today, "D047", "자동차용경유", "1880.08"),
                item(today, "K015", "자동차용부탄", "1135.19"))));
    }

    private OpinetAverageOilPriceItem item(String tradeDate, String productCode, String productName, String price) {
        try {
            return new ObjectMapper()
                    .readValue(
                            """
                            {
                              "TRADE_DT": "%s",
                              "PRODCD": "%s",
                              "PRODNM": "%s",
                              "PRICE": "%s",
                              "DIFF": "0.00"
                            }
                            """.formatted(tradeDate, productCode, productName, price), OpinetAverageOilPriceItem.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
