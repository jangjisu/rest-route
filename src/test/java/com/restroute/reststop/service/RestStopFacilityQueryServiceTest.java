package com.restroute.reststop.service;

import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoItem;
import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.reststop.client.response.HighwayServiceAreaInfoItem;
import com.restroute.reststop.controller.response.RestStopFacilityResponse;
import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RestStopFacilityQueryServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    private RestStopFacilityQueryService restStopFacilityQueryService;

    @BeforeEach
    void setUp() {
        restStopFacilityQueryService =
                new RestStopFacilityQueryService(restStopRepository, restStopRelatedInfoQueryService);
    }

    @Test
    @DisplayName("휴게소 기준 시설/주차 정보를 조회한다")
    void findByServiceAreaCode_returnsFacilities() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestStopDetailEntity detail = detail("수유실|쉼터", "O", "X");
        HighwayServiceAreaInfoEntity firstInfo = highwayServiceAreaInfo("10", "20", "1", "하행");
        HighwayServiceAreaInfoEntity secondInfo = highwayServiceAreaInfo("5", "7", "", "하행");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.of(detail),
                        List.of(firstInfo, secondInfo),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of()));

        Optional<RestStopFacilityResponse> result = restStopFacilityQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().convenienceFacilities()).containsExactly("수유실", "쉼터");
        assertThat(result.get().hasMaintenance()).isTrue();
        assertThat(result.get().allowsTruckParking()).isFalse();
        assertThat(result.get().direction()).isEqualTo("하행");
        assertThat(result.get().compactCarParkingCount()).isEqualTo(15);
        assertThat(result.get().fullSizeCarParkingCount()).isEqualTo(27);
        assertThat(result.get().disabledParkingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("휴게소가 없으면 시설/주차 정보가 없다")
    void findByServiceAreaCode_returnsEmptyWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<RestStopFacilityResponse> result = restStopFacilityQueryService.findByServiceAreaCode("UNKNOWN");

        assertThat(result).isEmpty();
        verify(restStopRelatedInfoQueryService, never()).findByRestStop(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("상세와 영업시설 데이터가 없으면 null 필드가 있는 시설/주차 응답을 반환한다")
    void findByServiceAreaCode_returnsNullFieldsWhenRelatedDataMissing() {
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

        Optional<RestStopFacilityResponse> result = restStopFacilityQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().convenienceFacilities()).isEmpty();
        assertThat(result.get().hasMaintenance()).isNull();
        assertThat(result.get().allowsTruckParking()).isNull();
        assertThat(result.get().direction()).isNull();
        assertThat(result.get().compactCarParkingCount()).isNull();
        assertThat(result.get().fullSizeCarParkingCount()).isNull();
        assertThat(result.get().disabledParkingCount()).isNull();
    }

    @Test
    @DisplayName("시설/주차 원천 문자열이 비어 있으면 null 필드가 있는 응답을 반환한다")
    void findByServiceAreaCode_returnsNullFieldsWhenSourceTextIsEmpty() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestStopDetailEntity detail = detail(" ", "\t", "");
        HighwayServiceAreaInfoEntity info = highwayServiceAreaInfo(null, " ", "", null);
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.of(detail),
                        List.of(info),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of()));

        Optional<RestStopFacilityResponse> result = restStopFacilityQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().convenienceFacilities()).isEmpty();
        assertThat(result.get().hasMaintenance()).isNull();
        assertThat(result.get().allowsTruckParking()).isNull();
        assertThat(result.get().direction()).isNull();
        assertThat(result.get().compactCarParkingCount()).isNull();
        assertThat(result.get().fullSizeCarParkingCount()).isNull();
        assertThat(result.get().disabledParkingCount()).isNull();
    }

    @Test
    @DisplayName("점검/화물차 여부가 O/X가 아니면 알 수 없음을 뜻하는 null을 반환한다")
    void findByServiceAreaCode_returnsNullBooleanWhenYnValueIsUnrecognized() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestStopDetailEntity detail = detail("수유실", "Y", "N");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.of(detail),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of()));

        Optional<RestStopFacilityResponse> result = restStopFacilityQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().hasMaintenance()).isNull();
        assertThat(result.get().allowsTruckParking()).isNull();
    }

    private RestStopDetailEntity detail(String convenience, String maintenanceYn, String truckSaYn) {
        var item = restStopDetailItem("A00001", "서울만남(부산)휴게소");
        ReflectionTestUtils.setField(item, "convenience", convenience);
        ReflectionTestUtils.setField(item, "maintenanceYn", maintenanceYn);
        ReflectionTestUtils.setField(item, "truckSaYn", truckSaYn);
        return RestStopDetailEntity.from(item);
    }

    private HighwayServiceAreaInfoEntity highwayServiceAreaInfo(
            String compactCarParkingCount,
            String fullSizeCarParkingCount,
            String disabledParkingCount,
            String direction) {
        HighwayServiceAreaInfoItem item = highwayServiceAreaInfoItem("000001", "서울만남주유소");
        ReflectionTestUtils.setField(item, "businessFacilityCode", "A00001");
        ReflectionTestUtils.setField(item, "compactCarParkingCount", compactCarParkingCount);
        ReflectionTestUtils.setField(item, "fullSizeCarParkingCount", fullSizeCarParkingCount);
        ReflectionTestUtils.setField(item, "disabledParkingCount", disabledParkingCount);
        ReflectionTestUtils.setField(item, "directionTypeName", direction);
        return HighwayServiceAreaInfoEntity.from(item);
    }
}
