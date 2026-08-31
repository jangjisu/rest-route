package com.restroute.reststop.service.backfill;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopUsageSnapshotEntity;
import com.restroute.reststop.domain.SizeTier;
import com.restroute.reststop.repository.RestStopUsageSnapshotRepository;
import com.restroute.reststop.service.usage.dto.RestStopUsageSnapshotRow;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopUsageSnapshotBackfillerTest {

    @Mock
    private RestStopUsageSnapshotRepository usageSnapshotRepository;

    private RestStopUsageSnapshotBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestStopUsageSnapshotBackfiller(usageSnapshotRepository);
    }

    private RestStopUsageSnapshotEntity snapshot(String name, String dailyTrafficVolume) {
        return RestStopUsageSnapshotEntity.from(
                new RestStopUsageSnapshotRow("경부선", name, "10000", "임대", "1000", dailyTrafficVolume));
    }

    @Test
    @DisplayName("비어 있는 매핑만 유일한 휴게소명으로 연결하고 개수를 센다")
    void backfillNames_mapsOnlyUnmappedSnapshotsByUniqueName() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopUsageSnapshotEntity unmapped = snapshot("서울만남(부산)", "5000");
        RestStopUsageSnapshotEntity alreadyMapped = snapshot("서울만남(부산)", "6000");
        alreadyMapped.updateRestStopServiceAreaCode("MANUAL");
        when(usageSnapshotRepository.findAll()).thenReturn(List.of(unmapped, alreadyMapped));

        int mappedCount = backfiller.backfillNames(List.of(restStop));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(unmapped.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(alreadyMapped.getRestStopServiceAreaCode()).isEqualTo("MANUAL");
    }

    @Test
    @DisplayName("이름이 모호하게 여러 휴게소와 일치하면 건드리지 않는다")
    void backfillNames_skipsAmbiguousNames() {
        RestStopEntity first = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopEntity second = RestStopEntity.from(restStopItem("002", "서울만남(부산)휴게소", "A00002"));
        RestStopUsageSnapshotEntity ambiguous = snapshot("서울만남(부산)", "5000");
        when(usageSnapshotRepository.findAll()).thenReturn(List.of(ambiguous));

        int mappedCount = backfiller.backfillNames(List.of(first, second));

        assertThat(mappedCount).isZero();
        assertThat(ambiguous.getRestStopServiceAreaCode()).isEmpty();
    }

    @Test
    @DisplayName("통행량 상위 10%만 topTrafficTier를 true로 표시한다")
    void recomputeTopTrafficTier_flagsOnlyTopTenPercent() {
        // 10개 중 상위 10% = 1개(올림)만 true가 되어야 한다.
        List<RestStopUsageSnapshotEntity> snapshots = List.of(
                snapshot("A", "1000"),
                snapshot("B", "900"),
                snapshot("C", "800"),
                snapshot("D", "700"),
                snapshot("E", "600"),
                snapshot("F", "500"),
                snapshot("G", "400"),
                snapshot("H", "300"),
                snapshot("I", "200"),
                snapshot("J", "100"));
        when(usageSnapshotRepository.findAll()).thenReturn(snapshots);

        backfiller.recomputeTopTrafficTier();

        assertThat(snapshots.get(0).isTopTrafficTier()).isTrue();
        for (int i = 1; i < snapshots.size(); i++) {
            assertThat(snapshots.get(i).isTopTrafficTier()).as("index %d", i).isFalse();
        }
    }

    @Test
    @DisplayName("통행량 값을 숫자로 해석할 수 없는 스냅샷은 태그 대상에서 제외한다")
    void recomputeTopTrafficTier_excludesUnparsableTraffic() {
        RestStopUsageSnapshotEntity valid = snapshot("A", "1000");
        RestStopUsageSnapshotEntity invalid = snapshot("B", "알수없음");
        when(usageSnapshotRepository.findAll()).thenReturn(List.of(valid, invalid));

        backfiller.recomputeTopTrafficTier();

        assertThat(valid.isTopTrafficTier()).isTrue();
        assertThat(invalid.isTopTrafficTier()).isFalse();
    }

    private RestStopUsageSnapshotEntity snapshotWithArea(String name, String siteAreaSqm) {
        return RestStopUsageSnapshotEntity.from(
                new RestStopUsageSnapshotRow("경부선", name, siteAreaSqm, "임대", "1000", "5000"));
    }

    @Test
    @DisplayName("부지면적 상위 25%는 LARGE, 하위 25%는 SMALL, 나머지는 MEDIUM으로 표시한다")
    void recomputeSizeTier_bucketsByAreaPercentile() {
        // 10개 중 상위 25% = 3개(올림) LARGE, 하위 25% = 3개(올림) SMALL, 나머지 4개 MEDIUM.
        List<RestStopUsageSnapshotEntity> snapshots = List.of(
                snapshotWithArea("A", "100000"),
                snapshotWithArea("B", "90000"),
                snapshotWithArea("C", "80000"),
                snapshotWithArea("D", "70000"),
                snapshotWithArea("E", "60000"),
                snapshotWithArea("F", "50000"),
                snapshotWithArea("G", "40000"),
                snapshotWithArea("H", "30000"),
                snapshotWithArea("I", "20000"),
                snapshotWithArea("J", "10000"));
        when(usageSnapshotRepository.findAll()).thenReturn(snapshots);

        backfiller.recomputeSizeTier();

        assertThat(snapshots.get(0).getSizeTier()).isEqualTo(SizeTier.LARGE);
        assertThat(snapshots.get(1).getSizeTier()).isEqualTo(SizeTier.LARGE);
        assertThat(snapshots.get(2).getSizeTier()).isEqualTo(SizeTier.LARGE);
        assertThat(snapshots.get(3).getSizeTier()).isEqualTo(SizeTier.MEDIUM);
        assertThat(snapshots.get(4).getSizeTier()).isEqualTo(SizeTier.MEDIUM);
        assertThat(snapshots.get(5).getSizeTier()).isEqualTo(SizeTier.MEDIUM);
        assertThat(snapshots.get(6).getSizeTier()).isEqualTo(SizeTier.MEDIUM);
        assertThat(snapshots.get(7).getSizeTier()).isEqualTo(SizeTier.SMALL);
        assertThat(snapshots.get(8).getSizeTier()).isEqualTo(SizeTier.SMALL);
        assertThat(snapshots.get(9).getSizeTier()).isEqualTo(SizeTier.SMALL);
    }

    @Test
    @DisplayName("부지면적 값을 숫자로 해석할 수 없는 스냅샷은 등급을 null로 둔다")
    void recomputeSizeTier_excludesUnparsableArea() {
        RestStopUsageSnapshotEntity valid = snapshotWithArea("A", "100000");
        RestStopUsageSnapshotEntity invalid = snapshotWithArea("B", "알수없음");
        when(usageSnapshotRepository.findAll()).thenReturn(List.of(valid, invalid));

        backfiller.recomputeSizeTier();

        assertThat(valid.getSizeTier()).isNotNull();
        assertThat(invalid.getSizeTier()).isNull();
    }

    @Test
    @DisplayName("전체가 1건이면 LARGE로 분류한다(경계 케이스)")
    void recomputeSizeTier_singleSnapshotBecomesLarge() {
        RestStopUsageSnapshotEntity only = snapshotWithArea("A", "50000");
        when(usageSnapshotRepository.findAll()).thenReturn(List.of(only));

        backfiller.recomputeSizeTier();

        assertThat(only.getSizeTier()).isEqualTo(SizeTier.LARGE);
    }

    @Test
    @DisplayName("빈 목록이면 예외 없이 아무 것도 하지 않는다")
    void recomputeSizeTier_doesNothingWhenEmpty() {
        when(usageSnapshotRepository.findAll()).thenReturn(List.of());

        backfiller.recomputeSizeTier();
    }
}
