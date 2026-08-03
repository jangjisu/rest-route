package com.restroute.service.admindashboard;

import com.restroute.domain.RestStopProductSalesRankEntity;
import com.restroute.domain.RestStopStoreSalesRankEntity;
import com.restroute.repository.RestStopProductSalesRankRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.repository.RestStopStoreSalesRankRepository;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admindashboard.dto.AdminActivityLogItemResponse;
import com.restroute.service.admindashboard.dto.AdminDashboardSummary;
import java.util.List;
import java.util.Optional;
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
        return AdminDashboardSummary.of(
                restStopRepository.count(), latestSalesRankingMonth(), "준비중", recentActivityLogs());
    }

    private String latestSalesRankingMonth() {
        return Stream.of(productLastMonth(), storeLastMonth())
                .flatMap(Optional::stream)
                .filter(StringUtils::hasText)
                .max(String::compareTo)
                .orElse(null);
    }

    private Optional<String> productLastMonth() {
        return productSalesRankRepository
                .findTopByOrderByBaseYearMonthDesc()
                .map(RestStopProductSalesRankEntity::getBaseYearMonth);
    }

    private Optional<String> storeLastMonth() {
        return storeSalesRankRepository
                .findTopByOrderByBaseYearMonthDesc()
                .map(RestStopStoreSalesRankEntity::getBaseYearMonth);
    }

    private List<AdminActivityLogItemResponse> recentActivityLogs() {
        return adminActivityLogService.findRecent().stream()
                .map(AdminActivityLogItemResponse::from)
                .toList();
    }
}
