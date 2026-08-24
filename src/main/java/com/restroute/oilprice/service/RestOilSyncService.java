package com.restroute.oilprice.service;

import com.restroute.client.ExApiClient;
import com.restroute.oilprice.client.response.RestOilItem;
import com.restroute.oilprice.client.response.RestOilResponse;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.repository.RestOilRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestOilSyncService {

    private final ExApiClient exApiClient;
    private final RestOilRepository restOilRepository;
    private final TransactionTemplate transactionTemplate;

    public int initializeRestOilsIfEmpty() {
        if (restOilRepository.count() > 0) {
            return 0;
        }

        return refreshRestOils();
    }

    public int refreshRestOils() {
        List<RestOilItem> items = fetchRestOils();

        transactionTemplate.executeWithoutResult(status -> upsertRestOils(items));

        return items.size();
    }

    private List<RestOilItem> fetchRestOils() {
        RestOilResponse response = exApiClient.getRestOilList();
        if (response.getList() == null) {
            return List.of();
        }

        return response.getList();
    }

    private void upsertRestOils(List<RestOilItem> items) {
        Map<String, RestOilEntity> existingByKey = restOilRepository.findAll().stream()
                .collect(Collectors.toMap(
                        entity -> oilKey(entity.getStandardRestCode(), entity.getConvenienceCode()),
                        entity -> entity,
                        (first, second) -> first));

        List<RestOilEntity> toSave = new ArrayList<>();
        for (RestOilItem item : items) {
            toSave.add(upsertOne(item, existingByKey));
        }

        restOilRepository.saveAll(toSave);
    }

    private RestOilEntity upsertOne(RestOilItem item, Map<String, RestOilEntity> existingByKey) {
        String key = oilKey(item.getStandardRestCode(), item.getConvenienceCode());
        RestOilEntity existing = existingByKey.get(key);

        if (existing != null) {
            existing.updateFrom(item);
            return existing;
        }

        return existingByKey.computeIfAbsent(key, k -> RestOilEntity.from(item));
    }

    private String oilKey(String standardRestCode, String convenienceCode) {
        return standardRestCode + "\n" + convenienceCode;
    }
}
