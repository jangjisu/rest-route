package com.restroute.oilprice.service;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.repository.RestOilRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestOilServiceAreaCodeBackfillerTest {

    @Mock
    private RestOilRepository restOilRepository;

    private RestOilServiceAreaCodeBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestOilServiceAreaCodeBackfiller(restOilRepository);
    }

    @Test
    @DisplayName("노선코드+정규화된 주유소명 키로 조회 맵에 있는 행만 채우고 개수를 센다")
    void backfill_mapsMatchingRowsAndCountsThem() {
        RestOilEntity matched = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilEntity unmatched = RestOilEntity.from(restOilItem("999999", "미매칭주유소"));
        when(restOilRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false))
                .thenReturn(List.of(matched, unmatched));
        String key = "0010\n" + RestOilEntity.normalizeStationName("서울만남(부산)주유소");

        int mappedCount = backfiller.backfill(Map.of(key, "A00001"));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(matched.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(unmatched.getRestStopServiceAreaCode()).isNull();
    }

    @Test
    @DisplayName("관리자가 연결을 잠근 행은 override=false 조회 자체에서 걸러져 대상에 포함되지 않는다")
    void backfill_queriesOnlyNonOverriddenRows() {
        when(restOilRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false))
                .thenReturn(List.of());
        String key = "0010\n" + RestOilEntity.normalizeStationName("서울만남(부산)주유소");

        int mappedCount = backfiller.backfill(Map.of(key, "A00001"));

        assertThat(mappedCount).isZero();
        verify(restOilRepository).findByRestStopServiceAreaCodesAndAdminOverridden(null, false);
    }
}
