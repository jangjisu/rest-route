package com.restroute.service.admindashboard.dto;

import java.util.List;

public record AdminDashboardSummary(
        long restStopCount,
        String latestSalesRankingMonth,
        String lastSyncStatus,
        List<AdminActivityLogItemResponse> recentActivityLogs) {

    public static AdminDashboardSummary of(
            long restStopCount,
            String latestSalesRankingMonth,
            String lastSyncStatus,
            List<AdminActivityLogItemResponse> recentActivityLogs) {
        return new AdminDashboardSummary(restStopCount, latestSalesRankingMonth, lastSyncStatus, recentActivityLogs);
    }
}
