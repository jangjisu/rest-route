package com.restroute.oilprice.service;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.oilprice.controller.response.OilInfoResponse;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.RestStopRelatedInfoQueryService;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopOilInfoQueryServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    private RestStopOilInfoQueryService restStopOilInfoQueryService;

    @BeforeEach
    void setUp() {
        restStopOilInfoQueryService =
                new RestStopOilInfoQueryService(restStopRepository, restStopRelatedInfoQueryService);
    }

    @Test
    @DisplayName("휴게소 기준 주유 편의시설과 주유 가격을 조회한다")
    void findByServiceAreaCode_returnsOilInfo() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(),
                        List.of(convenience),
                        Optional.of("000002"),
                        Optional.of(oilPrice),
                        List.of(),
                        List.of(),
                        List.of()));

        Optional<OilInfoResponse> result = restStopOilInfoQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().oilCompany()).isEqualTo("AD");
        assertThat(result.get().gasolinePrice()).isEqualTo("1,999원");
        assertThat(result.get().oilStationConveniences()).hasSize(1);
    }

    @Test
    @DisplayName("휴게소가 없으면 주유 정보가 없다")
    void findByServiceAreaCode_returnsEmptyWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<OilInfoResponse> result = restStopOilInfoQueryService.findByServiceAreaCode("UNKNOWN");

        assertThat(result).isEmpty();
        verify(restStopRelatedInfoQueryService, never()).findByRestStop(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("주유소 매핑이 없으면 주유 정보가 없다")
    void findByServiceAreaCode_returnsEmptyWhenOilMappingMissing() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of()));

        Optional<OilInfoResponse> result = restStopOilInfoQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isEmpty();
    }
}
