package com.restroute.reststop.service.backfill;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopUsageSnapshotEntity;
import com.restroute.reststop.repository.RestStopUsageSnapshotRepository;
import com.restroute.reststop.service.backfill.util.RestStopUniqueNameMatcher;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RestStopUsageSnapshotBackfiller {

    // 상위 10% 기준 — 사용자와 논의해 "혼잡 휴게소" 태그 하나만 쓰기로 결정함(다단계 등급은 애매한
    // 경계선만 늘어나 오히려 혼란스러움).
    private static final double TOP_TRAFFIC_TIER_RATIO = 0.1;

    private final RestStopUsageSnapshotRepository usageSnapshotRepository;

    public int backfillNames(List<RestStopEntity> restStops) {
        int mappedCount = 0;
        for (RestStopUsageSnapshotEntity usageSnapshot : usageSnapshotRepository.findAll()) {
            String serviceAreaCode = matchServiceAreaCode(usageSnapshot, restStops);
            if (serviceAreaCode == null) {
                continue;
            }
            usageSnapshot.updateRestStopServiceAreaCode(serviceAreaCode);
            mappedCount++;
        }
        return mappedCount;
    }

    /**
     * 전체 스냅샷을 통행량 내림차순으로 정렬해 상위 10%만 topTrafficTier를 true로 표시한다.
     * 조회 시점마다 순위를 다시 계산하지 않도록, CSV를 다시 올리거나 백필을 재실행할 때만 갱신된다.
     */
    public void recomputeTopTrafficTier() {
        List<RestStopUsageSnapshotEntity> all = usageSnapshotRepository.findAll();
        List<RestStopUsageSnapshotEntity> ranked = all.stream()
                .filter(snapshot -> parseTraffic(snapshot) != null)
                .sorted(Comparator.comparingInt((RestStopUsageSnapshotEntity snapshot) -> parseTraffic(snapshot))
                        .reversed())
                .toList();
        int topCount = (int) Math.ceil(ranked.size() * TOP_TRAFFIC_TIER_RATIO);
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).updateTopTrafficTier(i < topCount);
        }
        all.stream()
                .filter(snapshot -> parseTraffic(snapshot) == null)
                .forEach(snapshot -> snapshot.updateTopTrafficTier(false));
    }

    private Integer parseTraffic(RestStopUsageSnapshotEntity snapshot) {
        String value = snapshot.getDailyTrafficVolume();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String matchServiceAreaCode(RestStopUsageSnapshotEntity usageSnapshot, List<RestStopEntity> restStops) {
        if (usageSnapshot.isMapped()) {
            return null;
        }
        return RestStopUniqueNameMatcher.findUniqueServiceAreaCode(restStops, usageSnapshot.getSourceRestStopName());
    }
}
