package com.restroute.route.service;

import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.dto.AverageOilPrice;
import com.restroute.oilprice.dto.FuelType;
import com.restroute.oilprice.dto.FuelTypeSelection;
import com.restroute.oilprice.dto.NationalOilPriceSummary;
import com.restroute.route.controller.response.FuelPriceTier;
import com.restroute.route.service.dto.QueriedOilPriceStats;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RouteRestStopFuelTierCalculatorTest {

    private final RouteRestStopFuelTierCalculator calculator = new RouteRestStopFuelTierCalculator();

    /**
     * {@code getPriceByFuelType}가 실제 엔티티 필드를 그대로 읽으므로 mock이 아니라 실제 엔티티를
     * 만들고 필드만 원하는 값으로 덮어써야 한다 — mock은 스텁 안 한 필드 접근까지는 대신해주지 못한다.
     */
    private RestOilPriceEntity oilPrice(String gasoline, String diesel, String lpg) {
        RestOilPriceEntity entity = RestOilPriceEntity.from(restOilPriceItem("000001", "테스트주유소"));
        ReflectionTestUtils.setField(entity, "gasolinePrice", gasoline);
        ReflectionTestUtils.setField(entity, "dieselPrice", diesel);
        ReflectionTestUtils.setField(entity, "lpgPrice", lpg);
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
    @DisplayName("fuelType이 null이면 다른 유종이 조건을 만족해도 항상 null이다")
    void tier_returnsNullWhenFuelTypeIsNull() {
        RestOilPriceEntity oilPrice = oilPrice("1,790원", "1,950원", "1,150원");
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1790, 1880, 1100);

        FuelPriceTier tier = calculator.tier(FuelTypeSelection.NONE, Optional.of(oilPrice), stats, Optional.empty());

        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("유가 정보 자체가 없으면 null이다")
    void tier_returnsNullWhenNoOilPrice() {
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1790, null, null);

        FuelPriceTier tier =
                calculator.tier(FuelTypeSelection.of(FuelType.GASOLINE), Optional.empty(), stats, Optional.empty());

        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("선택한 유종 가격이 그 유종의 조회 최저가와 같으면 CHEAPEST다 — 다른 유종이 더 싸도 무관")
    void tier_returnsCheapestWhenSelectedFuelMatchesItsOwnQueriedMinimum() {
        RestOilPriceEntity oilPrice = oilPrice("1,950원", "1,880원", "1,100원");
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1950, 1880, 900);

        FuelPriceTier tier =
                calculator.tier(FuelTypeSelection.of(FuelType.DIESEL), Optional.of(oilPrice), stats, Optional.empty());

        assertThat(tier).isEqualTo(FuelPriceTier.CHEAPEST);
    }

    @Test
    @DisplayName("최저가는 아니지만 선택한 유종이 전국 평균보다 싸면 BELOW_AVERAGE다")
    void tier_returnsBelowAverageWhenCheaperThanNationalAverageButNotCheapest() {
        RestOilPriceEntity oilPrice = oilPrice("1,850원", null, null);
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1790, null, null);

        FuelPriceTier tier = calculator.tier(
                FuelTypeSelection.of(FuelType.GASOLINE),
                Optional.of(oilPrice),
                stats,
                nationalAverage("1,900원", null, null));

        assertThat(tier).isEqualTo(FuelPriceTier.BELOW_AVERAGE);
    }

    @Test
    @DisplayName("다른 유종이 최저가/평균보다 저렴이어도 선택한 유종이 둘 다 아니면 null이다")
    void tier_ignoresOtherFuelTypesEvenWhenTheyWouldQualify() {
        RestOilPriceEntity oilPrice = oilPrice("1,950원", "1,700원", "1,000원");
        QueriedOilPriceStats stats = new QueriedOilPriceStats(1700, 1700, 1000);

        FuelPriceTier tier = calculator.tier(
                FuelTypeSelection.of(FuelType.GASOLINE),
                Optional.of(oilPrice),
                stats,
                nationalAverage("1,900원", null, null));

        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("최저가도 아니고 전국 평균보다 비싸면 null이다")
    void tier_returnsNullWhenNeitherCheapestNorBelowAverage() {
        RestOilPriceEntity oilPrice = oilPrice(null, "1,950원", null);
        QueriedOilPriceStats stats = new QueriedOilPriceStats(null, 1790, null);

        FuelPriceTier tier = calculator.tier(
                FuelTypeSelection.of(FuelType.DIESEL),
                Optional.of(oilPrice),
                stats,
                nationalAverage(null, "1,900원", null));

        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("전국 평균 데이터 자체가 없으면 최저가가 아닌 이상 BELOW_AVERAGE가 될 수 없다")
    void tier_returnsNullWhenNationalAverageMissingAndNotCheapest() {
        RestOilPriceEntity oilPrice = oilPrice(null, null, "1,150원");
        QueriedOilPriceStats stats = new QueriedOilPriceStats(null, null, 1000);

        FuelPriceTier tier =
                calculator.tier(FuelTypeSelection.of(FuelType.LPG), Optional.of(oilPrice), stats, Optional.empty());

        assertThat(tier).isNull();
    }
}
