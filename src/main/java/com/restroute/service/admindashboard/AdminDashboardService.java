package com.restroute.service.admindashboard;

import com.restroute.repository.RestStopProductSalesRankRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.repository.RestStopStoreSalesRankRepository;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admindashboard.dto.AdminActivityLogItemResponse;
import com.restroute.service.admindashboard.dto.AdminDashboardSummary;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final RestStopRepository restStopRepository;
    private final RestStopProductSalesRankRepository productSalesRankRepository;
    private final RestStopStoreSalesRankRepository storeSalesRankRepository;
    private final AdminActivityLogService adminActivityLogService;

    public AdminDashboardSummary getSummary() {
        long restStopCount = restStopRepository.count();
        String latestSalesRankingMonth = Stream.of(
                        productSalesRankRepository
                                .findTopByOrderByBaseYearMonthDesc()
                                .map(entity -> entity.getBaseYearMonth())
                                .orElse(null),
                        storeSalesRankRepository
                                .findTopByOrderByBaseYearMonthDesc()
                                .map(entity -> entity.getBaseYearMonth())
                                .orElse(null))
                .filter(StringUtils::hasText)
                .max(String::compareTo)
                .orElse(null);
        List<AdminActivityLogItemResponse> recentActivityLogs = adminActivityLogService.findRecent().stream()
                .map(AdminActivityLogItemResponse::from)
                .toList();
        return AdminDashboardSummary.of(restStopCount, latestSalesRankingMonth, "준비중", recentActivityLogs);
    }
}
