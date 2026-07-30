package com.restroute.service.backfill;

import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.repository.HighwayServiceAreaInfoRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HighwayServiceAreaInfoServiceAreaCodeBackfillerTest {

    @Mock
    private HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;

    private HighwayServiceAreaInfoServiceAreaCodeBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new HighwayServiceAreaInfoServiceAreaCodeBackfiller(highwayServiceAreaInfoRepository);
    }

    @Test
    @DisplayName("영업시설 코드가 휴게소 코드 목록에 있으면 채우고 개수를 센다")
    void backfill_mapsMatchingRowsAndCountsThem() {
        HighwayServiceAreaInfoEntity matched =
                HighwayServiceAreaInfoEntity.from(highwayServiceAreaInfoItem("SA001", "서울만남(부산)휴게소"));
        ReflectionTestUtils.setField(matched, "businessFacilityCode", "A00001");
        HighwayServiceAreaInfoEntity unmatched =
                HighwayServiceAreaInfoEntity.from(highwayServiceAreaInfoItem("SA999", "미매칭휴게소"));
        ReflectionTestUtils.setField(unmatched, "businessFacilityCode", "A99999");
        when(highwayServiceAreaInfoRepository.findAll()).thenReturn(List.of(matched, unmatched));

        int mappedCount = backfiller.backfill(List.of("A00001"));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(matched.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(unmatched.getRestStopServiceAreaCode()).isNull();
    }
}
