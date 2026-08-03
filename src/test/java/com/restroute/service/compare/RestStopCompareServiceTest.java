package com.restroute.service.compare;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.restroute.controller.response.RestStopCompareResponse;
import com.restroute.controller.response.RestStopCompareResponse.RestStopCompareResult;
import com.restroute.controller.response.RestStopCompareResponse.RestStopCompareSide;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.RestStopRelatedInfo;
import com.restroute.service.RestStopRelatedInfoQueryService;
import com.restroute.service.image.RestStopImageQueryService;
import com.restroute.service.image.RestStopNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopCompareServiceTest {

    private RestStopRepository restStopRepository;
    private RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;
    private RestStopImageQueryService restStopImageQueryService;
    private RestStopCompareService service;

    @BeforeEach
    void setUp() {
        restStopRepository = mock(RestStopRepository.class);
        restStopRelatedInfoQueryService = mock(RestStopRelatedInfoQueryService.class);
        restStopImageQueryService = mock(RestStopImageQueryService.class);
        service = new RestStopCompareService(
                restStopRepository, restStopRelatedInfoQueryService, restStopImageQueryService);
    }

    @Test
    @DisplayName("두 휴게소의 유가/주차/부대시설을 비교해 항목별 승자와 종합 추천 side를 계산한다")
    void compare_returnsSidesAndWinners() {
        RestStopEntity restStopA = RestStopEntity.from(restStopItem("001", "888안성(서울)휴게소", "A00001"));
        RestStopEntity restStopB = RestStopEntity.from(restStopItem("002", "죽전(부산)복합휴게소", "A00002"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStopA));
        when(restStopRepository.findByServiceAreaCode("A00002")).thenReturn(Optional.of(restStopB));

        RestStopRelatedInfo infoA = relatedInfo("1798", "1689", "1186", 312, "수유실|샤워실|수면실|세탁실|약국");
        RestStopRelatedInfo infoB = relatedInfo("1872", "1720", "1140", 201, "수유실|약국");
        when(restStopRelatedInfoQueryService.findByRestStop(restStopA)).thenReturn(infoA);
        when(restStopRelatedInfoQueryService.findByRestStop(restStopB)).thenReturn(infoB);
        when(restStopImageQueryService.findExistingServiceAreaCodes(Set.of("A00001", "A00002")))
                .thenReturn(Set.of("A00001"));

        RestStopCompareResponse response = service.compare("A00001", "A00002");

        RestStopCompareSide sideA = response.sideA();
        assertThat(sideA.unitName()).isEqualTo("888안성(서울)휴게소");
        assertThat(sideA.routeName()).isEqualTo("경부선");
        assertThat(sideA.listImageUrl()).isEqualTo("/api/rest-stops/A00001/images/list");
        assertThat(sideA.gasolinePrice()).isEqualTo("1798");
        assertThat(sideA.parkingCount()).isEqualTo(312);
        assertThat(sideA.facilities()).containsExactly("수유실", "샤워실", "수면실", "세탁실", "약국");

        RestStopCompareSide sideB = response.sideB();
        assertThat(sideB.listImageUrl()).isNull();
        assertThat(sideB.facilities()).containsExactly("수유실", "약국");

        RestStopCompareResult result = response.result();
        assertThat(result.gasolineWinner()).isEqualTo("A");
        assertThat(result.dieselWinner()).isEqualTo("A");
        assertThat(result.lpgWinner()).isEqualTo("B");
        assertThat(result.parkingWinner()).isEqualTo("A");
        assertThat(result.facilityWinner()).isEqualTo("A");
        assertThat(result.recommendedSide()).isEqualTo("A");
    }

    @Test
    @DisplayName("부대시설은 rest_stop_detail.convenience만 반영하고 주유소 부대시설(rest_oil)은 반영하지 않는다")
    void compare_excludesOilStationConveniences() {
        RestStopEntity restStopA = RestStopEntity.from(restStopItem("001", "A휴게소", "A00001"));
        RestStopEntity restStopB = RestStopEntity.from(restStopItem("002", "B휴게소", "A00002"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStopA));
        when(restStopRepository.findByServiceAreaCode("A00002")).thenReturn(Optional.of(restStopB));

        RestStopDetailEntity detail = mock(RestStopDetailEntity.class);
        when(detail.getConvenience()).thenReturn("수유실");
        RestOilEntity oilConvenience = mock(RestOilEntity.class);
        when(oilConvenience.getConvenienceName()).thenReturn("샤워실");
        when(restStopRelatedInfoQueryService.findByRestStop(restStopA))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.of(detail),
                        List.of(),
                        List.of(oilConvenience),
                        Optional.empty(),
                        Optional.empty(),
                        List.of()));
        when(restStopRelatedInfoQueryService.findByRestStop(restStopB))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty(), List.of()));
        when(restStopImageQueryService.findExistingServiceAreaCodes(Set.of("A00001", "A00002")))
                .thenReturn(Set.of());

        RestStopCompareResponse response = service.compare("A00001", "A00002");

        assertThat(response.sideA().facilities()).containsExactly("수유실");
    }

    @Test
    @DisplayName("가격/주차/부대시설이 동률이면 그 항목은 승자가 없고, 종합 추천도 동률이면 없다")
    void compare_returnsNullWinnersWhenTied() {
        RestStopEntity restStopA = RestStopEntity.from(restStopItem("001", "A휴게소", "A00001"));
        RestStopEntity restStopB = RestStopEntity.from(restStopItem("002", "B휴게소", "A00002"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStopA));
        when(restStopRepository.findByServiceAreaCode("A00002")).thenReturn(Optional.of(restStopB));
        RestStopRelatedInfo infoA = relatedInfo("1800", "1700", "1150", 100, "수유실");
        RestStopRelatedInfo infoB = relatedInfo("1800", "1700", "1150", 100, "약국");
        when(restStopRelatedInfoQueryService.findByRestStop(restStopA)).thenReturn(infoA);
        when(restStopRelatedInfoQueryService.findByRestStop(restStopB)).thenReturn(infoB);
        when(restStopImageQueryService.findExistingServiceAreaCodes(Set.of("A00001", "A00002")))
                .thenReturn(Set.of());

        RestStopCompareResult result = service.compare("A00001", "A00002").result();

        assertThat(result.gasolineWinner()).isNull();
        assertThat(result.dieselWinner()).isNull();
        assertThat(result.lpgWinner()).isNull();
        assertThat(result.parkingWinner()).isNull();
        assertThat(result.facilityWinner()).isNull();
        assertThat(result.recommendedSide()).isNull();
    }

    @Test
    @DisplayName("가격/주차 정보가 없으면 해당 항목은 승자가 없다")
    void compare_returnsNullWinnersWhenDataMissing() {
        RestStopEntity restStopA = RestStopEntity.from(restStopItem("001", "A휴게소", "A00001"));
        RestStopEntity restStopB = RestStopEntity.from(restStopItem("002", "B휴게소", "A00002"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStopA));
        when(restStopRepository.findByServiceAreaCode("A00002")).thenReturn(Optional.of(restStopB));
        when(restStopRelatedInfoQueryService.findByRestStop(restStopA))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty(), List.of()));
        when(restStopRelatedInfoQueryService.findByRestStop(restStopB))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty(), List.of()));
        when(restStopImageQueryService.findExistingServiceAreaCodes(Set.of("A00001", "A00002")))
                .thenReturn(Set.of());

        RestStopCompareResponse response = service.compare("A00001", "A00002");

        assertThat(response.sideA().gasolinePrice()).isNull();
        assertThat(response.sideA().parkingCount()).isNull();
        assertThat(response.result().gasolineWinner()).isNull();
        assertThat(response.result().parkingWinner()).isNull();
        assertThat(response.result().recommendedSide()).isNull();
    }

    @Test
    @DisplayName("B가 유가/주차/부대시설에서 더 많이 이기면 종합 추천 side는 B다")
    void compare_recommendsSideBWhenBWinsMoreRows() {
        RestStopEntity restStopA = RestStopEntity.from(restStopItem("001", "A휴게소", "A00001"));
        RestStopEntity restStopB = RestStopEntity.from(restStopItem("002", "B휴게소", "A00002"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStopA));
        when(restStopRepository.findByServiceAreaCode("A00002")).thenReturn(Optional.of(restStopB));
        RestStopRelatedInfo infoA = relatedInfo("1900", "1700", "1150", 100, "수유실");
        RestStopRelatedInfo infoB = relatedInfo("1800", "1700", "1150", 200, "수유실|약국");
        when(restStopRelatedInfoQueryService.findByRestStop(restStopA)).thenReturn(infoA);
        when(restStopRelatedInfoQueryService.findByRestStop(restStopB)).thenReturn(infoB);
        when(restStopImageQueryService.findExistingServiceAreaCodes(Set.of("A00001", "A00002")))
                .thenReturn(Set.of());

        RestStopCompareResult result = service.compare("A00001", "A00002").result();

        assertThat(result.gasolineWinner()).isEqualTo("B");
        assertThat(result.parkingWinner()).isEqualTo("B");
        assertThat(result.facilityWinner()).isEqualTo("B");
        assertThat(result.recommendedSide()).isEqualTo("B");
    }

    @Test
    @DisplayName("가격이 한쪽만 없거나 숫자로 파싱할 수 없으면(판매하지 않음 등) 그 항목은 승자가 없다")
    void compare_returnsNullPriceWinnerWhenOnlyOneSideMissingOrUnparseable() {
        RestStopEntity restStopA = RestStopEntity.from(restStopItem("001", "A휴게소", "A00001"));
        RestStopEntity restStopB = RestStopEntity.from(restStopItem("002", "B휴게소", "A00002"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStopA));
        when(restStopRepository.findByServiceAreaCode("A00002")).thenReturn(Optional.of(restStopB));

        RestOilPriceEntity oilPriceA = mock(RestOilPriceEntity.class);
        when(oilPriceA.getGasolinePrice()).thenReturn("99999999999999");
        when(oilPriceA.getDieselPrice()).thenReturn("1,700원");
        when(oilPriceA.getLpgPrice()).thenReturn("X");
        RestOilPriceEntity oilPriceB = mock(RestOilPriceEntity.class);
        when(oilPriceB.getGasolinePrice()).thenReturn("1,800원");
        when(oilPriceB.getDieselPrice()).thenReturn(null);
        when(oilPriceB.getLpgPrice()).thenReturn("1,150원");
        when(restStopRelatedInfoQueryService.findByRestStop(restStopA))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.of(oilPriceA), List.of()));
        when(restStopRelatedInfoQueryService.findByRestStop(restStopB))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.of(oilPriceB), List.of()));
        when(restStopImageQueryService.findExistingServiceAreaCodes(Set.of("A00001", "A00002")))
                .thenReturn(Set.of());

        RestStopCompareResult result = service.compare("A00001", "A00002").result();

        assertThat(result.gasolineWinner()).isNull();
        assertThat(result.dieselWinner()).isNull();
        assertThat(result.lpgWinner()).isNull();
    }

    @Test
    @DisplayName("주차 대수가 한쪽만 없거나 필드값이 비어있거나 숫자가 아니거나 너무 크면 그 항목은 승자가 없다")
    void compare_returnsNullParkingWinnerForEdgeCaseParkingFields() {
        RestStopEntity restStopA = RestStopEntity.from(restStopItem("001", "A휴게소", "A00001"));
        RestStopEntity restStopB = RestStopEntity.from(restStopItem("002", "B휴게소", "A00002"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStopA));
        when(restStopRepository.findByServiceAreaCode("A00002")).thenReturn(Optional.of(restStopB));

        HighwayServiceAreaInfoEntity garbageParking = mock(HighwayServiceAreaInfoEntity.class);
        when(garbageParking.getCompactCarParkingCount()).thenReturn("99999999999999");
        when(garbageParking.getFullSizeCarParkingCount()).thenReturn("");
        when(garbageParking.getDisabledParkingCount()).thenReturn("없음");
        HighwayServiceAreaInfoEntity validParking = mock(HighwayServiceAreaInfoEntity.class);
        when(validParking.getCompactCarParkingCount()).thenReturn("50");
        when(validParking.getFullSizeCarParkingCount()).thenReturn("0");
        when(validParking.getDisabledParkingCount()).thenReturn("0");
        when(restStopRelatedInfoQueryService.findByRestStop(restStopA))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(garbageParking, validParking),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of()));
        when(restStopRelatedInfoQueryService.findByRestStop(restStopB))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty(), List.of()));
        when(restStopImageQueryService.findExistingServiceAreaCodes(Set.of("A00001", "A00002")))
                .thenReturn(Set.of());

        RestStopCompareResponse response = service.compare("A00001", "A00002");

        assertThat(response.sideA().parkingCount()).isEqualTo(50);
        assertThat(response.sideB().parkingCount()).isNull();
        assertThat(response.result().parkingWinner()).isNull();
    }

    @Test
    @DisplayName("같은 휴게소 코드를 두 번 넣으면 InvalidRestStopCompareException이 발생한다")
    void compare_throwsWhenSameServiceAreaCode() {
        assertThatThrownBy(() -> service.compare("A00001", "A00001"))
                .isInstanceOf(InvalidRestStopCompareException.class);
    }

    @Test
    @DisplayName("존재하지 않는 휴게소 코드면 RestStopNotFoundException이 발생한다")
    void compare_throwsWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.compare("UNKNOWN", "A00002")).isInstanceOf(RestStopNotFoundException.class);
    }

    private RestStopRelatedInfo relatedInfo(
            String gasolinePrice, String dieselPrice, String lpgPrice, int parkingCount, String convenience) {
        RestOilPriceEntity oilPrice = mock(RestOilPriceEntity.class);
        when(oilPrice.getGasolinePrice()).thenReturn(gasolinePrice);
        when(oilPrice.getDieselPrice()).thenReturn(dieselPrice);
        when(oilPrice.getLpgPrice()).thenReturn(lpgPrice);
        HighwayServiceAreaInfoEntity parking = mock(HighwayServiceAreaInfoEntity.class);
        when(parking.getCompactCarParkingCount()).thenReturn(String.valueOf(parkingCount));
        when(parking.getFullSizeCarParkingCount()).thenReturn("0");
        when(parking.getDisabledParkingCount()).thenReturn("0");
        RestStopDetailEntity detail = mock(RestStopDetailEntity.class);
        when(detail.getConvenience()).thenReturn(convenience);
        return RestStopRelatedInfo.of(
                Optional.of(detail), List.of(parking), List.of(), Optional.empty(), Optional.of(oilPrice), List.of());
    }
}
