package com.restroute.service.backfill;

import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.domain.RestStopDetailEntity;
import com.restroute.repository.RestStopDetailRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopDetailServiceAreaCodeBackfillerTest {

    @Mock
    private RestStopDetailRepository restStopDetailRepository;

    private RestStopDetailServiceAreaCodeBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestStopDetailServiceAreaCodeBackfiller(restStopDetailRepository);
    }

    @Test
    @DisplayName("휴게소 코드 목록에 있는 서비스지역코드만 채우고 매핑 개수를 센다")
    void backfill_mapsMatchingRowsAndCountsThem() {
        RestStopDetailEntity matched = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        RestStopDetailEntity unmatched = RestStopDetailEntity.from(restStopDetailItem("A99999", "미매칭휴게소"));
        when(restStopDetailRepository.findAll()).thenReturn(List.of(matched, unmatched));

        int mappedCount = backfiller.backfill(List.of("A00001"));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(matched.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(unmatched.getRestStopServiceAreaCode()).isNull();
    }

    @Test
    @DisplayName("매칭되는 행이 없으면 0을 반환한다")
    void backfill_returnsZeroWhenNoMatches() {
        RestStopDetailEntity unmatched = RestStopDetailEntity.from(restStopDetailItem("A99999", "미매칭휴게소"));
        when(restStopDetailRepository.findAll()).thenReturn(List.of(unmatched));

        int mappedCount = backfiller.backfill(List.of("A00001"));

        assertThat(mappedCount).isZero();
    }
}
