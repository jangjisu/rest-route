package com.restroute.reststop.service;

import com.restroute.reststop.controller.response.RestStopSalesRankingResponse;
import com.restroute.reststop.domain.RestStopProductSalesRankEntity;
import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import com.restroute.reststop.repository.RestStopProductSalesRankRepository;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopStoreSalesRankRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RestStopSalesRankingQueryService {

    private static final int TOP_RANKING_COUNT = 5;

    private final RestStopRepository restStopRepository;
    private final RestStopProductSalesRankRepository productSalesRankRepository;
    private final RestStopStoreSalesRankRepository storeSalesRankRepository;

    @Transactional(readOnly = true)
    public Optional<RestStopSalesRankingResponse> findByServiceAreaCode(String serviceAreaCode) {
        if (!StringUtils.hasText(serviceAreaCode)) {
            return Optional.empty();
        }

        if (restStopRepository.findByServiceAreaCode(serviceAreaCode).isEmpty()) {
            return Optional.empty();
        }

        List<RestStopProductSalesRankEntity> mappedProducts =
                productSalesRankRepository.findAllMappedProductsOrderByLatestMonth(serviceAreaCode);
        List<RestStopStoreSalesRankEntity> mappedStores =
                storeSalesRankRepository.findAllMappedStoresOrderByLatestMonth(serviceAreaCode);
        List<RestStopProductSalesRankEntity> validProducts =
                mappedProducts.stream().filter(this::hasValidRank).toList();
        List<RestStopStoreSalesRankEntity> validStores =
                mappedStores.stream().filter(this::hasValidRank).toList();
        if (validProducts.isEmpty() && validStores.isEmpty()) {
            return Optional.of(RestStopSalesRankingResponse.empty());
        }

        String latestMonth = latestMonth(validProducts, validStores);
        List<RestStopProductSalesRankEntity> topProducts = validProducts.stream()
                .filter(product -> latestMonth.equals(product.getBaseYearMonth()))
                .sorted(Comparator.comparingInt(product -> Integer.parseInt(product.getRestStopRank())))
                .limit(TOP_RANKING_COUNT)
                .toList();
        List<RestStopStoreSalesRankEntity> topStores = validStores.stream()
                .filter(store -> latestMonth.equals(store.getBaseYearMonth()))
                .sorted(Comparator.comparingInt(store -> Integer.parseInt(store.getRestStopRank())))
                .limit(TOP_RANKING_COUNT)
                .toList();
        return Optional.of(RestStopSalesRankingResponse.of(latestMonth, topStores, topProducts));
    }

    private String latestMonth(
            List<RestStopProductSalesRankEntity> products, List<RestStopStoreSalesRankEntity> stores) {
        return java.util.stream.Stream.concat(
                        products.stream().map(RestStopProductSalesRankEntity::getBaseYearMonth),
                        stores.stream().map(RestStopStoreSalesRankEntity::getBaseYearMonth))
                .max(String::compareTo)
                .orElse(null);
    }

    private boolean hasValidRank(RestStopProductSalesRankEntity product) {
        return hasValidRank(product.getBaseYearMonth(), product.getRestStopRank(), product.getProductName());
    }

    private boolean hasValidRank(RestStopStoreSalesRankEntity store) {
        return hasValidRank(store.getBaseYearMonth(), store.getRestStopRank(), store.getSourceStoreName());
    }

    private boolean hasValidRank(String baseYearMonth, String rank, String name) {
        if (!StringUtils.hasText(baseYearMonth) || !StringUtils.hasText(rank) || !StringUtils.hasText(name)) {
            return false;
        }

        try {
            return Integer.parseInt(rank) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
