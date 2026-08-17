package com.restroute.service.backfill;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.repository.RestOilPriceRepository;
import com.restroute.repository.RestOilRepository;
import java.util.List;
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

    @Mock
    private RestOilRepository restOilRepository;

    private RestOilPriceServiceAreaCodeBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestOilPriceServiceAreaCodeBackfiller(restOilPriceRepository, restOilRepository);
    }

    @Test
    @DisplayName("표준주유소코드로 조회 맵에 있는 가격 정보만 채우고 개수를 센다")
    void backfill_mapsMatchingRowsAndCountsThem() {
        RestOilPriceEntity matched = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity unmatched = RestOilPriceEntity.from(restOilPriceItem("999999", "미매칭주유소"));
        RestOilEntity canonicalOil = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        canonicalOil.updateRestStopServiceAreaCode("A00001");
        when(restOilPriceRepository.findAll()).thenReturn(List.of(matched, unmatched));
        when(restOilRepository.findAll()).thenReturn(List.of(canonicalOil));

        int mappedCount = backfiller.backfill();

        assertThat(mappedCount).isEqualTo(1);
        assertThat(matched.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(unmatched.getRestStopServiceAreaCode()).isNull();
    }

    @Test
    @DisplayName("가격 행의 기존 잠금 상태와 관계없이 rest_oil 기준 연결로 다시 채운다")
    void backfill_replacesDerivedLinkRegardlessOfPriceOverrideState() {
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        oilPrice.updateRestStopServiceAreaCode("OLD-CODE");
        RestOilEntity canonicalOil = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        canonicalOil.applyAdminLink("A00001");
        when(restOilPriceRepository.findAll()).thenReturn(List.of(oilPrice));
        when(restOilRepository.findAll()).thenReturn(List.of(canonicalOil));

        int mappedCount = backfiller.backfill();

        assertThat(mappedCount).isEqualTo(1);
        assertThat(oilPrice.getRestStopServiceAreaCode()).isEqualTo("A00001");
        verify(restOilPriceRepository).findAll();
    }
}
