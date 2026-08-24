package com.restroute.reststop.service.admindashboard.dto;

import java.util.List;

/**
 * lastSyncStatus는 실제 동기화 스케줄러/서비스의 상태를 추적하는 기능이 아직 없어 항상 null이다.
 * 값이 없을 때 화면에 보여줄 "준비중" 같은 placeholder 문구는 백엔드가 아니라 프런트엔드가 렌더링한다.
 */
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
