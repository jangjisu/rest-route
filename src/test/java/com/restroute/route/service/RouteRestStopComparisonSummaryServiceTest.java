package com.restroute.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.reststopcontent.domain.RestFoodEntity;
import com.restroute.route.controller.response.FuelPriceTier;
import com.restroute.route.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.route.controller.response.RouteRestStopResponse.ComparisonSummary;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.service.dto.QueriedOilPriceStats;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteRestStopComparisonSummaryServiceTest {

    @Test
    @DisplayName("휴게소 관련 정보로 가격 차이, 주차, 음식, 시설 비교 요약을 만든다")
    void create_returnsComparisonSummary() {
        RouteRestStopComparisonSummaryService service = new RouteRestStopComparisonSummaryService();

        RestOilPriceEntity oilPrice = mock(RestOilPriceEntity.class);
        when(oilPrice.getGasolinePrice()).thenReturn("1,850원");
        when(oilPrice.getDieselPrice()).thenReturn("1,900원");
        when(oilPrice.getLpgPrice()).thenReturn("1,135원");
        HighwayServiceAreaInfoEntity parking = mock(HighwayServiceAreaInfoEntity.class);
        when(parking.getCompactCarParkingCount()).thenReturn("10");
        when(parking.getFullSizeCarParkingCount()).thenReturn("5");
        when(parking.getDisabledParkingCount()).thenReturn("1");
        RestStopDetailEntity detail = mock(RestStopDetailEntity.class);
        when(detail.getConvenience()).thenReturn("수유실/쉼터, 쉼터");
        when(detail.getMaintenanceYn()).thenReturn("Y");
        when(detail.getTruckSaYn()).thenReturn("N");
        RestOilEntity oilConvenience = mock(RestOilEntity.class);
        when(oilConvenience.getConvenienceName()).thenReturn("샤워실");
        RestFoodEntity food = mock(RestFoodEntity.class);
        RestStopRelatedInfo relatedInfo = RestStopRelatedInfo.of(
                Optional.of(detail),
                List.of(parking),
                List.of(oilConvenience),
                Optional.empty(),
                Optional.of(oilPrice),
                List.of(food),
                List.of(),
                List.of());
        Optional<NationalOilPriceSummary> nationalOilPriceSummary = Optional.of(NationalOilPriceSummary.of(
                "2026.07.07",
                AverageOilPrice.of("B027", "휘발유", "1,893원", "-4.19"),
                AverageOilPrice.of("D047", "자동차용경유", "1,880원", "-4.51"),
                AverageOilPrice.of("K015", "자동차용부탄", "1,135원", "+0.01")));

        ComparisonSummary summary = service.create(relatedInfo, nationalOilPriceSummary);

        assertThat(summary.gasolinePrice()).isEqualTo("1,850원");
        assertThat(summary.gasolinePriceDiffFromAverage()).isEqualTo(-43);
        assertThat(summary.dieselPriceDiffFromAverage()).isEqualTo(20);
        assertThat(summary.lpgPriceDiffFromAverage()).isZero();
        assertThat(summary.totalParkingCount()).isEqualTo(16);
        assertThat(summary.foodMenuCount()).isEqualTo(1);
        assertThat(summary.facilityCount()).isEqualTo(4);
    }

    private RestOilPriceEntity oilPrice(String gasoline, String diesel, String lpg) {
        RestOilPriceEntity entity = mock(RestOilPriceEntity.class);
        lenient().when(entity.getGasolinePrice()).thenReturn(gasoline);
        lenient().when(entity.getDieselPrice()).thenReturn(diesel);
        lenient().when(entity.getLpgPrice()).thenReturn(lpg);
        return entity;
    }

    private Optional<NationalOilPriceSummary> nationalAverage(String gasoline, String diesel, String lpg) {
        return Optional.of(NationalOilPriceSummary.of(
                "2026.07.07",
                AverageOilPrice.of("B027", "휘발유", gasoline, "-4.19"),
                AverageOilPrice.of("D047", "자동차용경유", diesel, "-4.51"),
                AverageOilPrice.of("K015", "자동차용부탄", lpg, "+0.01")));
    }

    @Test
    @DisplayName("보유한 유종 중 하나라도 조회된 휴게소들 사이의 최저가와 같으면 CHEAPEST다")
    void fuelPriceTier_returnsCheapestWhenAnyFuelMatchesQueriedMinimum() {
        RouteRestStopComparisonSummaryService service = new RouteRestStopComparisonSummaryService();
        RestOilPriceEntity oilPrice = oilPrice("1,790원", "1,950원", "1,150원");
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1790, 1880, 1100);

        FuelPriceTier tier = service.fuelPriceTier(Optional.of(oilPrice), stats, Optional.empty());

        assertThat(tier).isEqualTo(FuelPriceTier.CHEAPEST);
    }

    @Test
    @DisplayName("조회된 최저가와는 안 같아도 보유 유종이 오늘자 전국 평균보다 싸면 BELOW_AVERAGE다")
    void fuelPriceTier_returnsBelowAverageWhenCheaperThanNationalAverageButNotCheapest() {
        RouteRestStopComparisonSummaryService service = new RouteRestStopComparisonSummaryService();
        RestOilPriceEntity oilPrice = oilPrice("1,850원", null, null);
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1790, null, null);

        FuelPriceTier tier = service.fuelPriceTier(Optional.of(oilPrice), stats, nationalAverage("1,900원", null, null));

        assertThat(tier).isEqualTo(FuelPriceTier.BELOW_AVERAGE);
    }

    @Test
    @DisplayName("최저가도 아니고 전국 평균보다 비싸면 등급이 없다(null)")
    void fuelPriceTier_returnsNullWhenNeitherCheapestNorBelowAverage() {
        RouteRestStopComparisonSummaryService service = new RouteRestStopComparisonSummaryService();
        RestOilPriceEntity oilPrice = oilPrice("1,950원", null, null);
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1790, null, null);

        FuelPriceTier tier = service.fuelPriceTier(Optional.of(oilPrice), stats, nationalAverage("1,900원", null, null));

        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("유가 정보 자체가 없으면 등급이 없다(null)")
    void fuelPriceTier_returnsNullWhenNoOilPrice() {
        RouteRestStopComparisonSummaryService service = new RouteRestStopComparisonSummaryService();
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1790, null, null);

        FuelPriceTier tier = service.fuelPriceTier(Optional.empty(), stats, Optional.empty());

        assertThat(tier).isNull();
    }
}
