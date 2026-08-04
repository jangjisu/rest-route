package com.restroute.service;

import com.restroute.client.ExApiClient;
import com.restroute.client.response.RestStopItem;
import com.restroute.client.response.RestStopResponse;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestStopSyncService {

    private static final int FIRST_PAGE = 1;

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
        RestStopResponse firstPage = fetchPageSafely(FIRST_PAGE);
        if (firstPage == null) {
            return List.of();
        }

        List<RestStopItem> items = new ArrayList<>();
        addItems(items, firstPage);

        int totalPageCount = firstPage.getTotalPageCount();
        for (int pageNo = FIRST_PAGE + 1; pageNo <= totalPageCount; pageNo++) {
            RestStopResponse response = fetchPageSafely(pageNo);
            if (response != null) {
                addItems(items, response);
            }
        }

        return items;
    }

    private RestStopResponse fetchPageSafely(int pageNo) {
        try {
            return exApiClient.getLocationInfoRest(pageNo);
        } catch (RuntimeException e) {
            log.warn("Rest stop page fetch failed. pageNo={}, cause={}", pageNo, e.getMessage(), e);
            return null;
        }
    }

    private void addItems(List<RestStopItem> items, RestStopResponse response) {
        if (response.getList() != null) {
            items.addAll(response.getList());
        }
    }

    private void upsertRestStops(List<RestStopItem> items) {
        Map<String, RestStopEntity> existingByKey = restStopRepository.findAll().stream()
                .collect(Collectors.toMap(
                        RestStopEntity::getServiceAreaCode, entity -> entity, (first, second) -> first));

        List<RestStopEntity> toSave = new ArrayList<>();
        for (RestStopItem item : items) {
            toSave.add(upsertOne(item, existingByKey));
        }

        restStopRepository.saveAll(toSave);
    }

    private RestStopEntity upsertOne(RestStopItem item, Map<String, RestStopEntity> existingByKey) {
        RestStopEntity existing = existingByKey.get(item.getServiceAreaCode());

        if (existing == null) {
            RestStopEntity created = RestStopEntity.from(item);
            existingByKey.put(item.getServiceAreaCode(), created);
            return created;
        }

        if (existing.isSyncable()) {
            existing.updateFrom(item);
        }
        return existing;
    }
}
