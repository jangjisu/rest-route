package com.restroute.reststop.service.backfill;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopUsageSnapshotEntity;
import com.restroute.reststop.domain.SizeTier;
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

    // 부지면적 상위/하위 25% — topTrafficTier와 같은 이유로 요청마다 계산하지 않고 백필 시점에 저장한다.
    private static final double LARGE_SIZE_TIER_RATIO = 0.25;
    private static final double SMALL_SIZE_TIER_RATIO = 0.25;

    /**
     * 전체 스냅샷을 부지면적 내림차순으로 정렬해 상위 25%는 LARGE, 하위 25%는 SMALL, 나머지는
     * MEDIUM으로 표시한다. recomputeTopTrafficTier()와 마찬가지로 CSV를 다시 올리거나 백필을
     * 재실행할 때만 갱신된다.
     */
    public void recomputeSizeTier() {
        List<RestStopUsageSnapshotEntity> all = usageSnapshotRepository.findAll();
        List<RestStopUsageSnapshotEntity> ranked = all.stream()
                .filter(snapshot -> parseSiteArea(snapshot) != null)
                .sorted(Comparator.comparingInt((RestStopUsageSnapshotEntity snapshot) -> parseSiteArea(snapshot))
                        .reversed())
                .toList();
        int largeCount = (int) Math.ceil(ranked.size() * LARGE_SIZE_TIER_RATIO);
        int smallCount = (int) Math.ceil(ranked.size() * SMALL_SIZE_TIER_RATIO);
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).updateSizeTier(sizeTierAt(i, ranked.size(), largeCount, smallCount));
        }
        all.stream()
                .filter(snapshot -> parseSiteArea(snapshot) == null)
                .forEach(snapshot -> snapshot.updateSizeTier(null));
    }

    private SizeTier sizeTierAt(int index, int totalCount, int largeCount, int smallCount) {
        if (index < largeCount) {
            return SizeTier.LARGE;
        }
        if (index >= totalCount - smallCount) {
            return SizeTier.SMALL;
        }
        return SizeTier.MEDIUM;
    }

    private Integer parseSiteArea(RestStopUsageSnapshotEntity snapshot) {
        String value = snapshot.getSiteAreaSqm();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
