package com.restroute.service;

import com.restroute.client.ExApiClient;
import com.restroute.client.response.RestEventItem;
import com.restroute.client.response.RestEventResponse;
import com.restroute.domain.RestEventEntity;
import com.restroute.repository.RestEventRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestEventSyncService {

    private final ExApiClient exApiClient;
    private final RestEventRepository restEventRepository;
    private final TransactionTemplate transactionTemplate;

    public int initializeRestEventsIfEmpty() {
        if (restEventRepository.count() > 0) {
            return 0;
        }

        return refreshRestEvents();
    }

    public int refreshRestEvents() {
        List<RestEventItem> items = fetchRestEvents();
        transactionTemplate.executeWithoutResult(status -> upsertRestEvents(items));
        return items.size();
    }

    private List<RestEventItem> fetchRestEvents() {
        RestEventResponse response = exApiClient.getRestEventList();
        if (response.getList() == null) {
            return List.of();
        }
        return response.getList();
    }

    private void upsertRestEvents(List<RestEventItem> items) {
        Map<String, RestEventEntity> existingByKey = restEventRepository.findAll().stream()
                .collect(Collectors.toMap(
                        entity -> eventKey(entity.getStdRestCd(), entity.getEventSeq()),
                        entity -> entity,
                        (first, second) -> first));

        List<RestEventEntity> toSave = new ArrayList<>();
        for (RestEventItem item : items) {
            toSave.add(upsertOne(item, existingByKey));
        }
        restEventRepository.saveAll(toSave);
    }

    private RestEventEntity upsertOne(RestEventItem item, Map<String, RestEventEntity> existingByKey) {
        String key = eventKey(item.getStdRestCd(), item.getEventSeq());
        RestEventEntity existing = existingByKey.get(key);
        if (existing == null) {
            RestEventEntity created = RestEventEntity.from(item);
            existingByKey.put(key, created);
            return created;
        }
        existing.updateFrom(item);
        return existing;
    }

    private String eventKey(String stdRestCd, String eventSeq) {
        return stdRestCd + "\n" + eventSeq;
    }
}
