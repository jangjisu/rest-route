package com.restroute.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.reststopcontent.domain.RestFoodEntity;
import com.restroute.route.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.route.controller.response.RouteRestStopResponse.ComparisonSummary;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
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
}
