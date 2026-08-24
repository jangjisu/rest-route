package com.restroute.reststopcontent.service;

import com.restroute.common.client.ExApiClient;
import com.restroute.common.sync.NaturalKeyUpserter;
import com.restroute.reststopcontent.client.response.RestEventItem;
import com.restroute.reststopcontent.client.response.RestEventResponse;
import com.restroute.reststopcontent.domain.RestEventEntity;
import com.restroute.reststopcontent.repository.RestEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestEventSyncService {

    private static final NaturalKeyUpserter<RestEventItem, String, RestEventEntity> UPSERTER = NaturalKeyUpserter.of(
            entity -> eventKey(entity.getStdRestCd(), entity.getEventSeq()),
            item -> eventKey(item.getStdRestCd(), item.getEventSeq()),
            RestEventEntity::from,
            RestEventEntity::updateFrom);

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
        List<RestEventEntity> toSave = UPSERTER.upsert(items, restEventRepository.findAll());
        restEventRepository.saveAll(toSave);
    }

    private static String eventKey(String stdRestCd, String eventSeq) {
        return stdRestCd + "\n" + eventSeq;
    }
}
