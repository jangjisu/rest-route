package com.restroute.evcharger.service.dto;

import com.restroute.evcharger.client.response.EvChargerItem;
import java.util.List;

public record EvChargerFetchSummary(
        List<EvChargerItem> items, int totalPageCount, int successfulPageCount, int failedPageCount) {

    public static EvChargerFetchSummary of(
            List<EvChargerItem> items, int totalPageCount, int successfulPageCount, int failedPageCount) {
        return new EvChargerFetchSummary(items, totalPageCount, successfulPageCount, failedPageCount);
    }
}
