package com.restroute.reststopcontent.service;

import com.restroute.common.client.ExApiClient;
import com.restroute.reststopcontent.client.response.RestBestfoodItem;
import com.restroute.reststopcontent.client.response.RestBestfoodResponse;
import com.restroute.reststopcontent.domain.RestFoodEntity;
import com.restroute.reststopcontent.repository.RestFoodRepository;
import com.restroute.service.sync.NaturalKeyUpserter;
import com.restroute.service.sync.PagedFetchTemplate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestFoodSyncService {

    private static final NaturalKeyUpserter<RestBestfoodItem, String, RestFoodEntity> UPSERTER = NaturalKeyUpserter.of(
            entity -> foodKey(entity.getStdRestCd(), entity.getSeq()),
            item -> foodKey(item.getStdRestCd(), item.getSeq()),
            RestFoodEntity::from,
            (existing, item) -> {
                if (existing.isSyncable()) {
                    existing.updateFrom(item);
                }
            });

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
        return PagedFetchTemplate.fetchAll(
                "Rest food",
                exApiClient::getRestBestfoodList,
                RestBestfoodResponse::getPageSize,
                RestBestfoodResponse::getList);
    }

    private void upsertRestFoods(List<RestBestfoodItem> items) {
        List<RestFoodEntity> toSave = UPSERTER.upsert(items, restFoodRepository.findAll());
        restFoodRepository.saveAll(toSave);
    }

    private static String foodKey(String stdRestCd, String seq) {
        return stdRestCd + "\n" + seq;
    }
}
