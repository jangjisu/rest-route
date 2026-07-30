package com.restroute.service.backfill;

import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.domain.RestOilPriceEntity;
import com.restroute.repository.RestOilPriceRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestOilPriceServiceAreaCodeBackfillerTest {

    @Mock
    private RestOilPriceRepository restOilPriceRepository;

    private RestOilPriceServiceAreaCodeBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestOilPriceServiceAreaCodeBackfiller(restOilPriceRepository);
    }

    @Test
    @DisplayName("표준주유소코드로 조회 맵에 있는 가격 정보만 채우고 개수를 센다")
    void backfill_mapsMatchingRowsAndCountsThem() {
        RestOilPriceEntity matched = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity unmatched = RestOilPriceEntity.from(restOilPriceItem("999999", "미매칭주유소"));
        when(restOilPriceRepository.findAll()).thenReturn(List.of(matched, unmatched));

        int mappedCount = backfiller.backfill(Map.of("000002", "A00001"));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(matched.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(unmatched.getRestStopServiceAreaCode()).isNull();
    }

    @Test
    @DisplayName("관리자가 연결을 잠근 행은 조회 맵에 매칭돼도 건드리지 않는다")
    void backfill_skipsAdminOverriddenRows() {
        RestOilPriceEntity locked = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        locked.applyAdminLink("A00099");
        when(restOilPriceRepository.findAll()).thenReturn(List.of(locked));

        int mappedCount = backfiller.backfill(Map.of("000002", "A00001"));

        assertThat(mappedCount).isZero();
        assertThat(locked.getRestStopServiceAreaCode()).isEqualTo("A00099");
    }
}
