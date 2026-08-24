package com.restroute.oilprice.service;

import com.restroute.common.client.ExApiClient;
import com.restroute.oilprice.client.response.RestOilPriceItem;
import com.restroute.oilprice.client.response.RestOilPriceResponse;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.repository.RestOilPriceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestOilPriceSyncService {

    private static final int FIRST_PAGE = 1;
    private static final int LAST_PAGE = 3;

    private final ExApiClient exApiClient;
    private final RestOilPriceRepository restOilPriceRepository;
    private final RestOilPriceServiceAreaCodeBackfiller restOilPriceBackfiller;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public int initializeRestOilPricesIfEmpty() {
        if (restOilPriceRepository.count() > 0) {
            return 0;
        }

        return refreshRestOilPrices();
    }

    public int refreshRestOilPrices() {
        List<RestOilPriceItem> items = new ArrayList<>();
        boolean allPagesFetched = fetchRestOilPrices(items);
        if (items.isEmpty() && !allPagesFetched) {
            return 0;
        }

        transactionTemplate.executeWithoutResult(status -> saveRestOilPrices(items, allPagesFetched));

        return items.size();
    }

    private boolean fetchRestOilPrices(List<RestOilPriceItem> items) {
        boolean allPagesFetched = true;
        for (int pageNo = FIRST_PAGE; pageNo <= LAST_PAGE; pageNo++) {
            RestOilPriceResponse response = fetchPageSafely(pageNo);
            if (response == null) {
                allPagesFetched = false;
                continue;
            }
            addItems(items, response);
        }

        return allPagesFetched;
    }

    private RestOilPriceResponse fetchPageSafely(int pageNo) {
        try {
            return exApiClient.getCurStateStation(pageNo);
        } catch (RuntimeException e) {
            log.warn("Rest oil price page fetch failed. pageNo={}, cause={}", pageNo, e.getMessage(), e);
            return null;
        }
    }

    private void addItems(List<RestOilPriceItem> items, RestOilPriceResponse response) {
        if (response.getList() == null) {
            return;
        }

        items.addAll(response.getList());
    }

    private void saveRestOilPrices(List<RestOilPriceItem> items, boolean allPagesFetched) {
        if (!allPagesFetched) {
            upsertRestOilPrices(items);
            restOilPriceBackfiller.backfill();
            return;
        }

        restOilPriceRepository.deleteAllInBatch();
        LocalDateTime refreshedAt = LocalDateTime.now(clock);
        restOilPriceRepository.saveAllAndFlush(items.stream()
                .map(item -> RestOilPriceEntity.from(item, refreshedAt))
                .toList());
        restOilPriceBackfiller.backfill();
    }

    private void upsertRestOilPrices(List<RestOilPriceItem> items) {
        LocalDateTime refreshedAt = LocalDateTime.now(clock);
        restOilPriceRepository.saveAllAndFlush(
                items.stream().map(item -> upsertOne(item, refreshedAt)).toList());
    }

    private RestOilPriceEntity upsertOne(RestOilPriceItem item, LocalDateTime refreshedAt) {
        return restOilPriceRepository
                .findByServiceAreaCode2(item.getServiceAreaCode2())
                .map(existing -> {
                    existing.updateFrom(item, refreshedAt);
                    return existing;
                })
                .orElseGet(() -> RestOilPriceEntity.from(item, refreshedAt));
    }
}
