package com.restroute.oilprice.service;

import com.restroute.oilprice.client.OpinetApiClient;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceItem;
import com.restroute.oilprice.domain.NationalOilPriceEntity;
import com.restroute.oilprice.dto.AverageOilPrice;
import com.restroute.oilprice.dto.NationalOilPriceSummary;
import com.restroute.oilprice.repository.NationalOilPriceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class NationalOilPriceService {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final OpinetApiClient opinetApiClient;
    private final NationalOilPriceRepository nationalOilPriceRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * 호출자가 readOnly 트랜잭션 안에서 부르는 경우(근처 휴게소 조회)가 있어, 저장은 호출자
     * 트랜잭션에 합류하지 않고 항상 새 쓰기 트랜잭션에서 커밋/롤백한다.
     */
    public NationalOilPriceService(
            OpinetApiClient opinetApiClient,
            NationalOilPriceRepository nationalOilPriceRepository,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.opinetApiClient = opinetApiClient;
        this.nationalOilPriceRepository = nationalOilPriceRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    public Optional<NationalOilPriceSummary> getTodaySummary() {
        LocalDate today = LocalDate.now(clock);
        List<NationalOilPriceEntity> todayPrices = nationalOilPriceRepository.findAllByTradeDate(today);
        if (hasTodayPrices(todayPrices)) {
            return summaryOf(todayPrices);
        }
        return fetchAndSaveTodayPrices(today).flatMap(this::summaryOf);
    }

    private Optional<List<NationalOilPriceEntity>> fetchAndSaveTodayPrices(LocalDate today) {
        try {
            List<OpinetAverageOilPriceItem> items =
                    opinetApiClient.getAverageOilPrices().oil();
            transactionTemplate.execute(status -> saveFetchedItems(items));
            return Optional.of(nationalOilPriceRepository.findAllByTradeDate(today));
        } catch (RuntimeException e) {
            log.warn("National oil price summary unavailable. tradeDate={}, message={}", today, e.getMessage());
            return Optional.empty();
        }
    }

    private Integer saveFetchedItems(List<OpinetAverageOilPriceItem> items) {
        List<NationalOilPriceEntity> entities =
                items.stream().map(NationalOilPriceEntity::from).toList();
        if (entities.isEmpty()) {
            return 0;
        }

        LocalDate tradeDate = entities.get(0).getTradeDate();
        nationalOilPriceRepository.deleteAllByTradeDate(tradeDate);
        nationalOilPriceRepository.saveAll(entities);
        return entities.size();
    }

    private boolean hasTodayPrices(List<NationalOilPriceEntity> prices) {
        Map<String, NationalOilPriceEntity> byProductCode = byProductCode(prices);
        return byProductCode.containsKey(Product.GASOLINE.code())
                && byProductCode.containsKey(Product.DIESEL.code())
                && byProductCode.containsKey(Product.LPG.code());
    }

    private boolean isMissingAnyProduct(List<NationalOilPriceEntity> prices) {
        return !hasTodayPrices(prices);
    }

    private Optional<NationalOilPriceSummary> summaryOf(List<NationalOilPriceEntity> prices) {
        if (isMissingAnyProduct(prices)) {
            return Optional.empty();
        }

        Map<String, NationalOilPriceEntity> byProductCode = byProductCode(prices);
        NationalOilPriceEntity gasoline = byProductCode.get(Product.GASOLINE.code());
        NationalOilPriceEntity diesel = byProductCode.get(Product.DIESEL.code());
        NationalOilPriceEntity lpg = byProductCode.get(Product.LPG.code());
        return Optional.of(NationalOilPriceSummary.of(
                gasoline.getTradeDate().format(DISPLAY_DATE_FORMAT),
                averageOilPriceOf(gasoline),
                averageOilPriceOf(diesel),
                averageOilPriceOf(lpg)));
    }

    private Map<String, NationalOilPriceEntity> byProductCode(List<NationalOilPriceEntity> prices) {
        return prices.stream()
                .collect(Collectors.toMap(
                        NationalOilPriceEntity::getProductCode, Function.identity(), (first, ignored) -> first));
    }

    private AverageOilPrice averageOilPriceOf(NationalOilPriceEntity entity) {
        return AverageOilPrice.of(
                entity.getProductCode(), entity.getProductName(), entity.formattedPrice(), entity.getDiff());
    }

    @RequiredArgsConstructor
    private enum Product {
        GASOLINE("B027"),
        DIESEL("D047"),
        LPG("K015");

        private final String code;

        private String code() {
            return code;
        }
    }
}
