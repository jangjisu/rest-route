package com.restroute.reststop.service;

import com.restroute.common.client.ExApiClient;
import com.restroute.reststop.client.response.RestStopDetailItem;
import com.restroute.reststop.client.response.RestStopDetailResponse;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.repository.RestStopDetailRepository;
import com.restroute.service.sync.NaturalKeyUpserter;
import com.restroute.service.sync.PagedFetchTemplate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestStopDetailSyncService {

    private static final NaturalKeyUpserter<RestStopDetailItem, String, RestStopDetailEntity> UPSERTER =
            NaturalKeyUpserter.of(
                    RestStopDetailEntity::getServiceAreaCode,
                    RestStopDetailItem::getServiceAreaCode,
                    RestStopDetailEntity::from,
                    (existing, item) -> {
                        if (existing.isSyncable()) {
                            existing.updateFrom(item);
                        }
                    });

    private final ExApiClient exApiClient;
    private final RestStopDetailRepository restStopDetailRepository;
    private final TransactionTemplate transactionTemplate;

    public int initializeRestStopDetailsIfEmpty() {
        if (restStopDetailRepository.count() > 0) {
            return 0;
        }

        return refreshRestStopDetails();
    }

    public int refreshRestStopDetails() {
        List<RestStopDetailItem> items = fetchAllRestStopDetails();

        transactionTemplate.executeWithoutResult(status -> upsertRestStopDetails(items));

        return items.size();
    }

    private List<RestStopDetailItem> fetchAllRestStopDetails() {
        return PagedFetchTemplate.fetchAll(
                "Rest stop detail",
                exApiClient::getConvenienceServiceArea,
                RestStopDetailResponse::getTotalPageCount,
                RestStopDetailResponse::getList);
    }

    private void upsertRestStopDetails(List<RestStopDetailItem> items) {
        List<RestStopDetailEntity> toSave = UPSERTER.upsert(items, restStopDetailRepository.findAll());
        restStopDetailRepository.saveAll(toSave);
    }
}
