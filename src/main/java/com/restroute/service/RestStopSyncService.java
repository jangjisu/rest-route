package com.restroute.service;

import com.restroute.client.ExApiClient;
import com.restroute.client.response.RestStopItem;
import com.restroute.client.response.RestStopResponse;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.sync.NaturalKeyUpserter;
import com.restroute.service.sync.PagedFetchTemplate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestStopSyncService {

    private static final NaturalKeyUpserter<RestStopItem, String, RestStopEntity> UPSERTER = NaturalKeyUpserter.of(
            RestStopEntity::getServiceAreaCode,
            RestStopItem::getServiceAreaCode,
            RestStopEntity::from,
            (existing, item) -> {
                if (existing.isSyncable()) {
                    existing.updateFrom(item);
                }
            });

    private final ExApiClient exApiClient;
    private final RestStopRepository restStopRepository;
    private final TransactionTemplate transactionTemplate;

    public int initializeRestStopsIfEmpty() {
        if (restStopRepository.count() > 0) {
            return 0;
        }

        return refreshRestStops();
    }

    public int refreshRestStops() {
        List<RestStopItem> items = fetchAllRestStops();

        transactionTemplate.executeWithoutResult(status -> upsertRestStops(items));

        return items.size();
    }

    private List<RestStopItem> fetchAllRestStops() {
        return PagedFetchTemplate.fetchAll(
                "Rest stop",
                exApiClient::getLocationInfoRest,
                RestStopResponse::getTotalPageCount,
                RestStopResponse::getList);
    }

    private void upsertRestStops(List<RestStopItem> items) {
        List<RestStopEntity> toSave = UPSERTER.upsert(items, restStopRepository.findAll());
        restStopRepository.saveAll(toSave);
    }
}
