package com.restroute.reststop.service;

import com.restroute.common.client.ExApiClient;
import com.restroute.reststop.client.response.HighwayServiceAreaInfoItem;
import com.restroute.reststop.client.response.HighwayServiceAreaInfoResponse;
import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.repository.HighwayServiceAreaInfoRepository;
import com.restroute.service.sync.NaturalKeyUpserter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class HighwayServiceAreaInfoSyncService {

    private static final NaturalKeyUpserter<HighwayServiceAreaInfoItem, String, HighwayServiceAreaInfoEntity> UPSERTER =
            NaturalKeyUpserter.of(
                    HighwayServiceAreaInfoEntity::getServiceAreaCode,
                    HighwayServiceAreaInfoItem::getServiceAreaCode,
                    HighwayServiceAreaInfoEntity::from,
                    HighwayServiceAreaInfoEntity::updateFrom);

    private final ExApiClient exApiClient;
    private final HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;
    private final TransactionTemplate transactionTemplate;

    public int refreshHighwayServiceAreaInfos() {
        List<HighwayServiceAreaInfoItem> items = fetchHighwayServiceAreaInfos();

        transactionTemplate.executeWithoutResult(status -> upsertHighwayServiceAreaInfos(items));

        return items.size();
    }

    private List<HighwayServiceAreaInfoItem> fetchHighwayServiceAreaInfos() {
        HighwayServiceAreaInfoResponse response = exApiClient.getHighwayServiceAreaInfoList();

        if (response.getList() == null) {
            return List.of();
        }

        return response.getList();
    }

    private void upsertHighwayServiceAreaInfos(List<HighwayServiceAreaInfoItem> items) {
        List<HighwayServiceAreaInfoEntity> toSave = UPSERTER.upsert(items, highwayServiceAreaInfoRepository.findAll());
        highwayServiceAreaInfoRepository.saveAll(toSave);
    }
}
