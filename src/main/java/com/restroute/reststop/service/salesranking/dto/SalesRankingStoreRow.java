package com.restroute.reststop.service.salesranking.dto;

public record SalesRankingStoreRow(
        String baseYearMonth,
        String overallRank,
        String restStopRank,
        String sourceRestStopCode,
        String sourceRestStopName,
        String sourceStoreCode,
        String sourceStoreName) {}
