package com.restroute.service;

import com.restroute.client.ExApiClient;
import com.restroute.client.response.RestBestfoodItem;
import com.restroute.client.response.RestBestfoodResponse;
import com.restroute.domain.RestFoodEntity;
import com.restroute.repository.RestFoodRepository;
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
public class RestFoodSyncService {

    private static final int FIRST_PAGE = 1;

    private final ExApiClient exApiClient;
    private final RestFoodRepository restFoodRepository;
    private final TransactionTemplate transactionTemplate;

    public int initializeRestFoodsIfEmpty() {
        if (restFoodRepository.count() > 0) {
            return 0;
        }

        return refreshRestFoods();
    }

    public int refreshRestFoods() {
        List<RestBestfoodItem> items = fetchRestFoods();

        transactionTemplate.executeWithoutResult(status -> upsertRestFoods(items));

        return items.size();
    }

    private List<RestBestfoodItem> fetchRestFoods() {
        List<RestBestfoodItem> items = new ArrayList<>();
        RestBestfoodResponse firstPage = fetchPageSafely(FIRST_PAGE);
        if (firstPage == null) {
            return List.of();
        }

        addItems(items, firstPage);

        int totalPages = firstPage.getPageSize();
        for (int pageNo = FIRST_PAGE + 1; pageNo <= totalPages; pageNo++) {
            RestBestfoodResponse response = fetchPageSafely(pageNo);
            if (response != null) {
                addItems(items, response);
            }
        }

        return items;
    }

    private RestBestfoodResponse fetchPageSafely(int pageNo) {
        try {
            return exApiClient.getRestBestfoodList(pageNo);
        } catch (RuntimeException e) {
            log.warn("Rest food page fetch failed. pageNo={}, cause={}", pageNo, e.getMessage(), e);
            return null;
        }
    }

    private void addItems(List<RestBestfoodItem> items, RestBestfoodResponse response) {
        if (response.getList() == null) {
            return;
        }

        items.addAll(response.getList());
    }

    private void upsertRestFoods(List<RestBestfoodItem> items) {
        Map<String, RestFoodEntity> existingByKey = restFoodRepository.findAll().stream()
                .collect(Collectors.toMap(
                        entity -> foodKey(entity.getStdRestCd(), entity.getSeq()),
                        entity -> entity,
                        (first, second) -> first));

        List<RestFoodEntity> toSave = new ArrayList<>();
        for (RestBestfoodItem item : items) {
            toSave.add(upsertOne(item, existingByKey));
        }

        restFoodRepository.saveAll(toSave);
    }

    private RestFoodEntity upsertOne(RestBestfoodItem item, Map<String, RestFoodEntity> existingByKey) {
        String key = foodKey(item.getStdRestCd(), item.getSeq());
        RestFoodEntity existing = existingByKey.get(key);

        if (existing != null) {
            if (existing.isSyncable()) {
                existing.updateFrom(item);
            }
            return existing;
        }

        return existingByKey.computeIfAbsent(key, k -> RestFoodEntity.from(item));
    }

    private String foodKey(String stdRestCd, String seq) {
        return stdRestCd + "\n" + seq;
    }
}
