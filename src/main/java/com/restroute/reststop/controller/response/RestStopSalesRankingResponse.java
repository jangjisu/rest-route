package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopProductSalesRankEntity;
import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import java.util.List;

public record RestStopSalesRankingResponse(
        String baseYearMonth,
        List<RestStopSalesRankingStoreItemResponse> storeRankings,
        List<RestStopSalesRankingItemResponse> products) {

    public static RestStopSalesRankingResponse of(
            String baseYearMonth,
            List<RestStopStoreSalesRankEntity> stores,
            List<RestStopProductSalesRankEntity> products) {
        List<RestStopSalesRankingStoreItemResponse> storeItems =
                stores.stream().map(RestStopSalesRankingStoreItemResponse::from).toList();
        List<RestStopSalesRankingItemResponse> items =
                products.stream().map(RestStopSalesRankingItemResponse::from).toList();
        return new RestStopSalesRankingResponse(baseYearMonth, storeItems, items);
    }

    public static RestStopSalesRankingResponse empty() {
        return new RestStopSalesRankingResponse(null, List.of(), List.of());
    }
}
