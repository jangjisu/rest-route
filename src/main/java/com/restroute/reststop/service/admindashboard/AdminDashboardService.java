package com.restroute.reststop.service.admindashboard;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.reststop.domain.RestStopProductSalesRankEntity;
import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import com.restroute.reststop.repository.RestStopProductSalesRankRepository;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopStoreSalesRankRepository;
import com.restroute.reststop.service.admindashboard.dto.AdminActivityLogItemResponse;
import com.restroute.reststop.service.admindashboard.dto.AdminDashboardSummary;
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
                restStopRepository.count(), latestSalesRankingMonth(), null, recentActivityLogs());
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
