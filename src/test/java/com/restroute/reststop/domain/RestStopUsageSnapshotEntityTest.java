package com.restroute.reststop.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststop.service.usage.dto.RestStopUsageSnapshotRow;
import org.junit.jupiter.api.Test;

class RestStopUsageSnapshotEntityTest {

    @Test
    void updateSizeTier_setsAndReadsBackTheTier() {
        RestStopUsageSnapshotEntity entity = RestStopUsageSnapshotEntity.from(
                new RestStopUsageSnapshotRow("경부선", "천안삼거리", "10000", "임대", "500", "3000"));

        entity.updateSizeTier(SizeTier.LARGE);

        assertThat(entity.getSizeTier()).isEqualTo(SizeTier.LARGE);
    }

    @Test
    void newEntity_hasNullSizeTierUntilBackfilled() {
        RestStopUsageSnapshotEntity entity = RestStopUsageSnapshotEntity.from(
                new RestStopUsageSnapshotRow("경부선", "천안삼거리", "10000", "임대", "500", "3000"));

        assertThat(entity.getSizeTier()).isNull();
    }
}
