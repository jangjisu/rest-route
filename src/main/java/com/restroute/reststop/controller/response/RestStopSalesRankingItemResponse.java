package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopProductSalesRankEntity;

public record RestStopSalesRankingItemResponse(int rank, String productName) {

    public static RestStopSalesRankingItemResponse from(RestStopProductSalesRankEntity entity) {
        return new RestStopSalesRankingItemResponse(
                Integer.parseInt(entity.getRestStopRank()), entity.getProductName());
    }
}
