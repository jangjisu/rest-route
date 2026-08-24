package com.restroute.service.evcharger;

import com.restroute.client.EvChargerApiClient;
import com.restroute.client.ExternalApiRequestLog;
import com.restroute.client.response.EvChargerItem;
import com.restroute.client.response.EvChargerResponse;
import com.restroute.domain.EvChargerEntity;
import com.restroute.repository.EvChargerRepository;
import com.restroute.service.evcharger.dto.EvChargerFetchSummary;
import com.restroute.service.evcharger.dto.EvChargerSyncResult;
import com.restroute.service.sync.NaturalKeyUpserter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvChargerSyncService {

    private static final int FIRST_PAGE = 1;

    private static final NaturalKeyUpserter<EvChargerItem, String, EvChargerEntity> UPSERTER = NaturalKeyUpserter.of(
            entity -> chargerKey(entity.getStatId(), entity.getChgerId()),
            item -> chargerKey(item.getStatId(), item.getChgerId()),
            EvChargerEntity::from,
            EvChargerEntity::updateFrom);

    private final EvChargerApiClient evChargerApiClient;
    private final EvChargerRepository evChargerRepository;
    private final TransactionTemplate transactionTemplate;

    public EvChargerSyncResult initializeEvChargersIfEmpty() {
        if (evChargerRepository.count() > 0) {
            return EvChargerSyncResult.skipped();
        }
        return refreshEvChargers();
    }

    public EvChargerSyncResult refreshEvChargers() {
        EvChargerFetchSummary summary = fetchAllEvChargers();
        if (summary.items().isEmpty()) {
            EvChargerSyncResult result = resultOf(summary, 0, 0);
            log.warn(
                    "EV charger sync skipped because no successful C001 data was fetched. "
                            + "totalPageCount={}, successfulPageCount={}, failedPageCount={}",
                    result.totalPageCount(),
                    result.successfulPageCount(),
                    result.failedPageCount());
            return result;
        }

        transactionTemplate.executeWithoutResult(status -> upsertEvChargers(summary.items()));
        EvChargerSyncResult result = resultOf(summary, summary.items().size(), uniqueStationCount(summary.items()));
        log.info(
                "EV charger sync completed. totalPageCount={}, successfulPageCount={}, failedPageCount={}, "
                        + "savedItemCount={}, uniqueStationCount={}",
                result.totalPageCount(),
                result.successfulPageCount(),
                result.failedPageCount(),
                result.savedItemCount(),
                result.uniqueStationCount());
        return result;
    }

    private EvChargerFetchSummary fetchAllEvChargers() {
        EvChargerResponse firstPage = fetchPage(FIRST_PAGE);
        if (firstPage == null) {
            return EvChargerFetchSummary.of(List.of(), 0, 0, 1);
        }

        List<EvChargerItem> items = new ArrayList<>();
        addHighwayRestStopItems(items, firstPage);
        int totalPageCount = firstPage.getTotalPageCount();
        int successfulPageCount = 1;
        int failedPageCount = 0;

        for (int pageNo = FIRST_PAGE + 1; pageNo <= totalPageCount; pageNo++) {
            EvChargerResponse response = fetchPage(pageNo);
            if (response == null) {
                failedPageCount++;
                continue;
            }
            addHighwayRestStopItems(items, response);
            successfulPageCount++;
        }
        return EvChargerFetchSummary.of(items, totalPageCount, successfulPageCount, failedPageCount);
    }

    private EvChargerResponse fetchPage(int pageNo) {
        log.info("EV charger page request started. pageNo={}", pageNo);
        try {
            EvChargerResponse response = evChargerApiClient.getChargerInfo(pageNo);
            log.info(
                    "EV charger page request succeeded. pageNo={}, totalCount={}, itemCount={}, "
                            + "highwayRestStopItemCount={}",
                    pageNo,
                    response.getTotalCount(),
                    response.getList().size(),
                    highwayRestStopItemCount(response));
            return response;
        } catch (RuntimeException e) {
            log.warn(
                    "EV charger page request failed. pageNo={}, exceptionType={}, cause={}",
                    pageNo,
                    e.getClass().getSimpleName(),
                    safeMessage(e));
            return null;
        }
    }

    private void addHighwayRestStopItems(List<EvChargerItem> items, EvChargerResponse response) {
        response.getList().stream()
                .filter(EvChargerItem::isHighwayRestStop)
                .filter(EvChargerItem::hasChargerIdentity)
                .forEach(items::add);
    }

    private long highwayRestStopItemCount(EvChargerResponse response) {
        return response.getList().stream()
                .filter(EvChargerItem::isHighwayRestStop)
                .count();
    }

    private void upsertEvChargers(List<EvChargerItem> items) {
        List<EvChargerEntity> toSave = UPSERTER.upsert(items, evChargerRepository.findAll());
        evChargerRepository.saveAll(toSave);
    }

    private long uniqueStationCount(List<EvChargerItem> items) {
        return items.stream().map(EvChargerItem::getStatId).distinct().count();
    }

    private static String chargerKey(String statId, String chgerId) {
        return statId + "\n" + chgerId;
    }

    private EvChargerSyncResult resultOf(EvChargerFetchSummary summary, int savedItemCount, long uniqueStationCount) {
        return EvChargerSyncResult.of(
                summary.totalPageCount(),
                summary.successfulPageCount(),
                summary.failedPageCount(),
                savedItemCount,
                uniqueStationCount);
    }

    private String safeMessage(Throwable throwable) {
        return ExternalApiRequestLog.sanitizeUrl(throwable.getMessage());
    }
}
