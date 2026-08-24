package com.restroute.reststop.service.salesranking.dto;

public record SalesRankingProductRow(
        String baseYearMonth,
        String restStopRank,
        String sourceRestStopCode,
        String sourceRestStopName,
        String sourceStoreCode,
        String sourceStoreName,
        String productSequence,
        String productName) {}
