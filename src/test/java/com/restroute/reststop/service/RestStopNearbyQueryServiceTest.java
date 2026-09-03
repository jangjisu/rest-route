package com.restroute.reststop.service;

import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.service.NationalOilPriceService;
import com.restroute.reststop.controller.response.RestStopNearbyItemResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.SizeTier;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.route.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.dto.FuelType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RestStopNearbyQueryServiceTest {

    @Mock
    private RestStopQueryService restStopQueryService;

    @Mock
    private RestStopAggregateQueryService restStopAggregateQueryService;

    @Mock
    private EvChargerQueryService evChargerQueryService;

    @Mock
    private NationalOilPriceService nationalOilPriceService;

    private RestStopNearbyQueryService service;

    @BeforeEach
    void setUp() {
        service = new RestStopNearbyQueryService(
                restStopQueryService, restStopAggregateQueryService, evChargerQueryService, nationalOilPriceService);
        lenient()
                .when(restStopAggregateQueryService.findByRestStopsAndAdminOverridden(any(), any()))
                .thenReturn(Map.of());
    }

    private RestStopEntity restStop(String serviceAreaCode, String unitName, String yValue, String xValue) {
        RestStopEntity entity = RestStopEntity.from(restStopItem("001", unitName, serviceAreaCode));
        ReflectionTestUtils.setField(entity, "yValue", yValue);
        ReflectionTestUtils.setField(entity, "xValue", xValue);
        return entity;
    }

    private RestStopAggregate aggregate(
            RestStopRelatedInfo relatedInfo,
            boolean topTrafficTier,
            boolean hasTheme,
            boolean hasEvent,
            SizeTier sizeTier) {
        return new RestStopAggregate(
                null, relatedInfo, false, false, hasTheme, hasEvent, null, null, topTrafficTier, sizeTier);
    }

    private RestStopRelatedInfo relatedInfoWithOilPrice(Optional<RestOilPriceEntity> oilPrice) {
        return RestStopRelatedInfo.of(
                Optional.empty(), List.of(), List.of(), Optional.empty(), oilPrice, List.of(), List.of(), List.of());
    }

    @Test
    @DisplayName("파라미터가 없으면 전체 목록을 위치/유가 정보 없이 반환한다")
    void findNearby_returnsAllRestStopsWithoutDistanceOrInterestWhenNoParams() {
        RestStopEntity restStop = restStop("A00001", "서울만남(부산)휴게소", "37.5", "127.0");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        when(restStopAggregateQueryService.findByRestStopsAndAdminOverridden(List.of(restStop), null))
                .thenReturn(Map.of(
                        "A00001",
                        aggregate(relatedInfoWithOilPrice(Optional.empty()), true, true, false, SizeTier.LARGE)));

        List<RestStopNearbyItemResponse> result = service.findNearby(null, null, null, null);

        assertThat(result).hasSize(1);
        RestStopNearbyItemResponse item = result.get(0);
        assertThat(item.unitName()).isEqualTo("서울만남(부산)휴게소");
        assertThat(item.distanceMeters()).isNull();
        assertThat(item.evChargerCount()).isNull();
        assertThat(item.fuelBelowAverage()).isNull();
        assertThat(item.sizeTier()).isEqualTo(SizeTier.LARGE);
        assertThat(item.topTrafficTier()).isTrue();
        assertThat(item.hasTheme()).isTrue();
        assertThat(item.hasEvent()).isFalse();
    }

    @Test
    @DisplayName("이름이 있으면 이름 검색 결과만 대상으로 한다")
    void findNearby_filtersByNameWhenGiven() {
        RestStopEntity restStop = restStop("A00001", "안성(서울)휴게소", "37.5", "127.0");
        when(restStopQueryService.searchByName("안성")).thenReturn(List.of(restStop));

        List<RestStopNearbyItemResponse> result = service.findNearby(null, null, "안성", null);

        assertThat(result).extracting(RestStopNearbyItemResponse::unitName).containsExactly("안성(서울)휴게소");
    }

    @Test
    @DisplayName("위치가 있으면 거리를 계산해서 가까운 순으로 정렬한다")
    void findNearby_computesDistanceAndSortsAscending() {
        RestStopEntity far = restStop("A00001", "먼휴게소", "38.5", "128.0");
        RestStopEntity near = restStop("A00002", "가까운휴게소", "37.501", "127.001");
        when(restStopQueryService.findAll()).thenReturn(List.of(far, near));

        List<RestStopNearbyItemResponse> result = service.findNearby(37.5, 127.0, null, null);

        assertThat(result).extracting(RestStopNearbyItemResponse::unitName).containsExactly("가까운휴게소", "먼휴게소");
        assertThat(result.get(0).distanceMeters()).isLessThan(result.get(1).distanceMeters());
    }

    @Test
    @DisplayName("좌표를 숫자로 못 바꾸면 거리는 null이고 정렬에서 뒤로 밀린다")
    void findNearby_treatsUnparsableCoordinateAsNullDistance() {
        RestStopEntity broken = restStop("A00001", "좌표없음", "", "");
        RestStopEntity valid = restStop("A00002", "좌표있음", "37.5", "127.0");
        when(restStopQueryService.findAll()).thenReturn(List.of(broken, valid));

        List<RestStopNearbyItemResponse> result = service.findNearby(37.5, 127.0, null, null);

        assertThat(result).extracting(RestStopNearbyItemResponse::unitName).containsExactly("좌표있음", "좌표없음");
        assertThat(result.get(1).distanceMeters()).isNull();
    }

    @Test
    @DisplayName("관심 항목이 EV면 활성 충전기 수를 배치로 채우고, 없으면 null이다")
    void findNearby_returnsEvChargerCountOnlyWhenInterestIsEv() {
        RestStopEntity withCharger = restStop("A00001", "충전있음", "37.5", "127.0");
        RestStopEntity withoutCharger = restStop("A00002", "충전없음", "37.6", "127.1");
        when(restStopQueryService.findAll()).thenReturn(List.of(withCharger, withoutCharger));
        when(evChargerQueryService.findActiveChargerCounts(List.of("A00001", "A00002")))
                .thenReturn(Map.of("A00001", 8));

        List<RestStopNearbyItemResponse> result = service.findNearby(null, null, null, FuelType.EV);

        Map<String, Integer> countByName = new HashMap<>();
        result.forEach(item -> countByName.put(item.unitName(), item.evChargerCount()));
        assertThat(countByName.get("충전있음")).isEqualTo(8);
        assertThat(countByName.get("충전없음")).isNull();
    }

    @Test
    @DisplayName("관심 항목이 EV가 아니면 충전기 배치 조회 자체를 하지 않는다")
    void findNearby_skipsEvChargerLookupWhenInterestIsNotEv() {
        RestStopEntity restStop = restStop("A00001", "휴게소", "37.5", "127.0");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));

        service.findNearby(null, null, null, FuelType.GASOLINE);

        org.mockito.Mockito.verifyNoInteractions(evChargerQueryService);
    }

    private NationalOilPriceSummary nationalAverage(String gasoline, String diesel, String lpg) {
        return NationalOilPriceSummary.of(
                "2026.07.07",
                AverageOilPrice.of("B027", "휘발유", gasoline, "-4.19"),
                AverageOilPrice.of("D047", "자동차용경유", diesel, "-4.51"),
                AverageOilPrice.of("K015", "자동차용부탄", lpg, "+0.01"));
    }

    @Test
    @DisplayName("선택한 유종이 오늘자 전국 평균보다 싸면 true다")
    void findNearby_returnsTrueWhenSelectedFuelIsBelowNationalAverage() {
        RestStopEntity restStop = restStop("A00001", "휴게소", "37.5", "127.0");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000001", "테스트주유소"));
        ReflectionTestUtils.setField(oilPrice, "gasolinePrice", "1,700원");
        when(restStopAggregateQueryService.findByRestStopsAndAdminOverridden(List.of(restStop), null))
                .thenReturn(Map.of(
                        "A00001",
                        aggregate(relatedInfoWithOilPrice(Optional.of(oilPrice)), false, false, false, null)));
        when(nationalOilPriceService.getTodaySummary())
                .thenReturn(Optional.of(nationalAverage("1,900원", "1,800원", "1,100원")));

        List<RestStopNearbyItemResponse> result = service.findNearby(null, null, null, FuelType.GASOLINE);

        assertThat(result.get(0).fuelBelowAverage()).isTrue();
    }

    @Test
    @DisplayName("선택한 유종이 전국 평균보다 안 싸면 null이다(false를 따로 안 준다)")
    void findNearby_returnsNullWhenSelectedFuelIsNotBelowAverage() {
        RestStopEntity restStop = restStop("A00001", "휴게소", "37.5", "127.0");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000001", "테스트주유소"));
        ReflectionTestUtils.setField(oilPrice, "gasolinePrice", "2,000원");
        when(restStopAggregateQueryService.findByRestStopsAndAdminOverridden(List.of(restStop), null))
                .thenReturn(Map.of(
                        "A00001",
                        aggregate(relatedInfoWithOilPrice(Optional.of(oilPrice)), false, false, false, null)));
        when(nationalOilPriceService.getTodaySummary())
                .thenReturn(Optional.of(nationalAverage("1,900원", "1,800원", "1,100원")));

        List<RestStopNearbyItemResponse> result = service.findNearby(null, null, null, FuelType.GASOLINE);

        assertThat(result.get(0).fuelBelowAverage()).isNull();
    }

    @Test
    @DisplayName("전국 평균 데이터 자체가 없으면 null이다")
    void findNearby_returnsNullWhenNationalAverageMissing() {
        RestStopEntity restStop = restStop("A00001", "휴게소", "37.5", "127.0");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        when(nationalOilPriceService.getTodaySummary()).thenReturn(Optional.empty());

        List<RestStopNearbyItemResponse> result = service.findNearby(null, null, null, FuelType.DIESEL);

        assertThat(result.get(0).fuelBelowAverage()).isNull();
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 목록을 반환하고 다른 조회는 하지 않는다")
    void findNearby_returnsEmptyListWhenNoRestStopsFound() {
        when(restStopQueryService.searchByName("없는이름")).thenReturn(List.of());

        List<RestStopNearbyItemResponse> result = service.findNearby(null, null, "없는이름", null);

        assertThat(result).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(restStopAggregateQueryService);
    }
}
