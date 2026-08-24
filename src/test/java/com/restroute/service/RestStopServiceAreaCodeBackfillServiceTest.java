package com.restroute.service;

import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoItem;
import static com.restroute.support.RestStopTestFixtures.restEventItem;
import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static com.restroute.support.RestStopTestFixtures.restThemeItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.EvChargerItem;
import com.restroute.client.response.RestBestfoodItem;
import com.restroute.client.response.RestOilItem;
import com.restroute.domain.EvChargerEntity;
import com.restroute.domain.EvChargerStationMappingEntity;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestEventEntity;
import com.restroute.domain.RestFoodEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.domain.RestStopProductSalesRankEntity;
import com.restroute.domain.RestStopStoreSalesRankEntity;
import com.restroute.domain.RestThemeEntity;
import com.restroute.repository.EvChargerRepository;
import com.restroute.repository.EvChargerStationMappingRepository;
import com.restroute.repository.HighwayServiceAreaInfoRepository;
import com.restroute.repository.RestEventRepository;
import com.restroute.repository.RestFoodRepository;
import com.restroute.repository.RestOilPriceRepository;
import com.restroute.repository.RestOilRepository;
import com.restroute.repository.RestStopDetailRepository;
import com.restroute.repository.RestStopProductSalesRankRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.repository.RestStopStoreSalesRankRepository;
import com.restroute.repository.RestThemeRepository;
import com.restroute.service.backfill.EvChargerStationMappingBackfiller;
import com.restroute.service.backfill.HighwayServiceAreaInfoServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestEventServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestFoodServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestOilPriceServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestOilServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestStopDetailServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestStopProductSalesRankBackfiller;
import com.restroute.service.backfill.RestStopStoreSalesRankBackfiller;
import com.restroute.service.backfill.RestThemeServiceAreaCodeBackfiller;
import com.restroute.service.evcharger.mapping.EvChargerStationMappingCalculator;
import com.restroute.service.salesranking.dto.SalesRankingProductRow;
import com.restroute.service.salesranking.dto.SalesRankingStoreRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    RestStopServiceAreaCodeBackfillService.class,
    RestStopQueryService.class,
    EvChargerStationMappingCalculator.class,
    EvChargerStationMappingBackfiller.class,
    RestStopDetailServiceAreaCodeBackfiller.class,
    HighwayServiceAreaInfoServiceAreaCodeBackfiller.class,
    RestFoodServiceAreaCodeBackfiller.class,
    RestOilServiceAreaCodeBackfiller.class,
    RestOilPriceServiceAreaCodeBackfiller.class,
    RestStopProductSalesRankBackfiller.class,
    RestStopStoreSalesRankBackfiller.class,
    RestThemeServiceAreaCodeBackfiller.class,
    RestEventServiceAreaCodeBackfiller.class
})
class RestStopServiceAreaCodeBackfillServiceTest {

    @Autowired
    private RestStopServiceAreaCodeBackfillService backfillService;

    @Autowired
    private EvChargerRepository evChargerRepository;

    @Autowired
    private RestStopRepository restStopRepository;

    @Autowired
    private RestStopDetailRepository restStopDetailRepository;

    @Autowired
    private HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;

    @Autowired
    private RestFoodRepository restFoodRepository;

    @Autowired
    private RestOilRepository restOilRepository;

    @Autowired
    private RestOilPriceRepository restOilPriceRepository;

    @Autowired
    private EvChargerStationMappingRepository evChargerStationMappingRepository;

    @Autowired
    private RestStopProductSalesRankRepository productSalesRankRepository;

    @Autowired
    private RestStopStoreSalesRankRepository storeSalesRankRepository;

    @Autowired
    private RestThemeRepository restThemeRepository;

    @Autowired
    private RestEventRepository restEventRepository;

    @Test
    @DisplayName("기존 휴게소 관련 row에 rest_stop_service_area_code를 연결 규칙 순서대로 채운다")
    void backfill_mapsExistingRowsByRestStopServiceAreaCode() throws Exception {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")));
        restStopDetailRepository.save(RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소")));
        HighwayServiceAreaInfoEntity highwayServiceAreaInfo =
                HighwayServiceAreaInfoEntity.from(highwayServiceAreaInfoItem("SA001", "서울만남(부산)휴게소"));
        highwayServiceAreaInfo.updateRestStopServiceAreaCode("stale");
        ReflectionTestUtils.setField(highwayServiceAreaInfo, "businessFacilityCode", "A00001");
        highwayServiceAreaInfoRepository.save(highwayServiceAreaInfo);
        restFoodRepository.save(foodEntity("000001", "한우국밥"));
        restOilRepository.save(RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소")));
        restOilPriceRepository.save(RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소")));
        restThemeRepository.save(RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소")));
        restEventRepository.save(RestEventEntity.from(restEventItem("000001", "1665")));

        Map<String, Integer> result = backfillService.backfill();

        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_STOP_DETAIL_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.HIGHWAY_SERVICE_AREA_INFO_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_FOOD_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_OIL_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_OIL_PRICE_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_THEME_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_EVENT_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(restStopDetailRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
        assertThat(highwayServiceAreaInfoRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
        assertThat(restFoodRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
        assertThat(restOilRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
        assertThat(restOilPriceRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
        assertThat(restThemeRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
        assertThat(restEventRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
    }

    @Test
    @DisplayName("매핑되지 않는 기존 row는 삭제하지 않고 조회 키를 null로 유지한다")
    void backfill_keepsUnmatchedRowsWithNullLookupKey() throws Exception {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")));
        restStopDetailRepository.save(RestStopDetailEntity.from(restStopDetailItem("A99999", "미매칭휴게소")));
        HighwayServiceAreaInfoEntity highwayServiceAreaInfo =
                HighwayServiceAreaInfoEntity.from(highwayServiceAreaInfoItem("SA999", "미매칭휴게소"));
        ReflectionTestUtils.setField(highwayServiceAreaInfo, "businessFacilityCode", "A99999");
        highwayServiceAreaInfoRepository.save(highwayServiceAreaInfo);
        restFoodRepository.save(foodEntity("999999", "미매칭메뉴"));
        RestOilItem unmatchedOil = restOilItem("999999", "미매칭주유소");
        ReflectionTestUtils.setField(unmatchedOil, "routeCode", "9999");
        restOilRepository.save(RestOilEntity.from(unmatchedOil));
        restOilPriceRepository.save(RestOilPriceEntity.from(restOilPriceItem("999999", "미매칭주유소")));
        restThemeRepository.save(RestThemeEntity.from(restThemeItem("999999", "미매칭테마")));
        restEventRepository.save(RestEventEntity.from(restEventItem("999999", "1")));

        Map<String, Integer> result = backfillService.backfill();

        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_STOP_DETAIL_MAPPED_COUNT))
                .isZero();
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.HIGHWAY_SERVICE_AREA_INFO_MAPPED_COUNT))
                .isZero();
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_FOOD_MAPPED_COUNT))
                .isZero();
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_OIL_MAPPED_COUNT))
                .isZero();
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_OIL_PRICE_MAPPED_COUNT))
                .isZero();
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_THEME_MAPPED_COUNT))
                .isZero();
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_EVENT_MAPPED_COUNT))
                .isZero();
        assertThat(restStopDetailRepository.count()).isEqualTo(1);
        assertThat(restStopDetailRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isNull();
        assertThat(highwayServiceAreaInfoRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isNull();
        assertThat(restFoodRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isNull();
        assertThat(restOilRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isNull();
        assertThat(restOilPriceRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isNull();
        assertThat(restThemeRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isNull();
        assertThat(restEventRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isNull();
    }

    @Test
    @DisplayName("주유 가격 backfill은 주유소 편의시설에 먼저 채워진 조회 키를 사용한다")
    void backfill_mapsOilPriceFromRestOilLookupKey() {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")));
        restOilRepository.save(RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소")));
        restOilPriceRepository.save(RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소")));

        backfillService.backfill();

        assertThat(restOilRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
        assertThat(restOilPriceRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
    }

    @Test
    @DisplayName("관리자가 연결을 잠근 주유소는 배치가 다시 자동 매칭해도 값을 바꾸지 않는다")
    void backfill_skipsRestOilRowsLockedByAdmin() {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")));
        restStopRepository.save(RestStopEntity.from(restStopItem("002", "다른휴게소", "A00099")));
        RestOilEntity oil = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        oil.applyAdminLink("A00099");
        restOilRepository.save(oil);

        backfillService.backfill();

        RestOilEntity reloaded = restOilRepository.findAll().get(0);
        assertThat(reloaded.getRestStopServiceAreaCode()).isEqualTo("A00099");
        assertThat(reloaded.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("주유소 가격 연결은 관리자가 잠근 rest_oil 연결을 기준으로 다시 채운다")
    void backfill_derivesRestOilPriceLinkFromCanonicalRestOilRows() {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")));
        restStopRepository.save(RestStopEntity.from(restStopItem("002", "다른휴게소", "A00099")));
        RestOilEntity canonicalOil = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        canonicalOil.applyAdminLink("A00099");
        restOilRepository.save(canonicalOil);
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        oilPrice.updateRestStopServiceAreaCode("A00001");
        restOilPriceRepository.save(oilPrice);

        backfillService.backfill();

        RestOilPriceEntity reloaded = restOilPriceRepository.findAll().get(0);
        assertThat(reloaded.getRestStopServiceAreaCode()).isEqualTo("A00099");
    }

    @Test
    @DisplayName("조회 키 기준 repository 메서드는 기존 조회 전환 전에도 사용할 수 있다")
    void repositories_findByRestStopServiceAreaCode() throws Exception {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")));
        restStopDetailRepository.save(RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소")));
        HighwayServiceAreaInfoEntity highwayServiceAreaInfo =
                HighwayServiceAreaInfoEntity.from(highwayServiceAreaInfoItem("SA001", "서울만남(부산)휴게소"));
        ReflectionTestUtils.setField(highwayServiceAreaInfo, "businessFacilityCode", "A00001");
        highwayServiceAreaInfoRepository.save(highwayServiceAreaInfo);
        restFoodRepository.save(foodEntity("000001", "한우국밥"));
        restOilRepository.save(RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소")));
        restOilPriceRepository.save(RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소")));
        backfillService.backfill();

        assertThat(restStopDetailRepository.findByRestStopServiceAreaCode("A00001"))
                .isPresent();
        assertThat(highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCode("A00001"))
                .hasSize(1);
        assertThat(restFoodRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .hasSize(1);
        assertThat(restOilRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .hasSize(1);
        assertThat(restOilPriceRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .hasSize(1);
    }

    @Test
    @DisplayName("중복 매핑 후보가 있어도 첫 번째 매핑을 유지하고 backfill을 계속한다")
    void backfill_keepsFirstMappingWhenCandidatesAreDuplicated() throws Exception {
        restStopRepository.saveAll(List.of(
                RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")),
                RestStopEntity.from(restStopItem("002", "서울만남(부산)휴게소", "A00001"))));
        restStopDetailRepository.save(RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소")));
        restFoodRepository.save(foodEntity("000001", "한우국밥"));
        restOilRepository.saveAll(List.of(
                RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소", "07")),
                RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소", "08"))));
        restOilPriceRepository.save(RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소")));

        Map<String, Integer> result = backfillService.backfill();

        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_STOP_DETAIL_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_FOOD_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_OIL_MAPPED_COUNT))
                .isEqualTo(2);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.REST_OIL_PRICE_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(restOilPriceRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
    }

    @Test
    @DisplayName("중앙 backfill은 EV 매핑 클래스의 결과를 저장한다")
    void backfill_replacesEvMappingsWithMapperResult() {
        EvChargerStationMappingEntity existing = EvChargerStationMappingEntity.of("ME1");
        existing.updateMatch("A00001");
        evChargerStationMappingRepository.save(existing);

        Map<String, Integer> result = backfillService.backfill();

        assertThat(result.get(RestStopServiceAreaCodeBackfillService.EV_CHARGER_MAPPED_COUNT))
                .isZero();
        assertThat(evChargerStationMappingRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("중앙 backfill은 rest stop detail과 EV 충전소를 Calculator에 전달한다")
    void backfill_calculatesEvMappingFromThreeDataSources() throws Exception {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")));
        restStopDetailRepository.save(RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소")));
        evChargerRepository.save(chargerEntity("ME1", "서울만남(부산) 휴게소", "37.4600218", "127.0420378"));

        Map<String, Integer> result = backfillService.backfill();

        assertThat(result.get(RestStopServiceAreaCodeBackfillService.EV_CHARGER_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(evChargerStationMappingRepository.findAll())
                .singleElement()
                .extracting(EvChargerStationMappingEntity::getRestStopServiceAreaCode)
                .isEqualTo("A00001");
    }

    @Test
    @DisplayName("판매순위 backfill은 비어 있는 매핑만 유일한 휴게소명으로 연결한다")
    void backfill_mapsOnlyUnmappedSalesRanksByUniqueName() {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")));
        RestStopProductSalesRankEntity product = RestStopProductSalesRankEntity.from(
                new SalesRankingProductRow("2026-06", "1", "S000001", "서울만남(부산)휴게소", "M1", "매장", "P1", "상품"));
        RestStopStoreSalesRankEntity store = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "1", "1", "S000001", "서울만남(부산)휴게소", "M1", "매장"));
        productSalesRankRepository.save(product);
        storeSalesRankRepository.save(store);

        Map<String, Integer> result = backfillService.backfill();

        assertThat(result.get(RestStopServiceAreaCodeBackfillService.PRODUCT_SALES_RANK_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(result.get(RestStopServiceAreaCodeBackfillService.STORE_SALES_RANK_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(productSalesRankRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
        assertThat(storeSalesRankRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
    }

    @Test
    @DisplayName("판매순위 backfill은 이미 연결된 값과 중복 후보를 변경하지 않는다")
    void backfill_preservesMappedRanksAndSkipsAmbiguousNames() {
        restStopRepository.saveAll(List.of(
                RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001")),
                RestStopEntity.from(restStopItem("002", "서울만남(부산)휴게소", "A00002"))));
        RestStopProductSalesRankEntity mapped = RestStopProductSalesRankEntity.from(
                new SalesRankingProductRow("2026-06", "1", "S000001", "서울만남(부산)휴게소", "M1", "매장", "P1", "상품"));
        mapped.updateRestStopServiceAreaCode("MANUAL");
        RestStopStoreSalesRankEntity mappedStore = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "1", "1", "S000001", "서울만남(부산)휴게소", "M1", "매장"));
        mappedStore.updateRestStopServiceAreaCode("MANUAL_STORE");
        RestStopProductSalesRankEntity ambiguous = RestStopProductSalesRankEntity.from(
                new SalesRankingProductRow("2026-06", "2", "S000001", "서울만남(부산)휴게소", "M1", "매장", "P2", "상품2"));
        RestStopStoreSalesRankEntity ambiguousStore = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "2", "2", "S000001", "서울만남(부산)휴게소", "M1", "매장"));
        productSalesRankRepository.saveAll(List.of(mapped, ambiguous));
        storeSalesRankRepository.saveAll(List.of(mappedStore, ambiguousStore));

        Map<String, Integer> result = backfillService.backfill();

        assertThat(result.get(RestStopServiceAreaCodeBackfillService.PRODUCT_SALES_RANK_MAPPED_COUNT))
                .isZero();
        assertThat(productSalesRankRepository.findAll())
                .extracting(RestStopProductSalesRankEntity::getRestStopServiceAreaCode)
                .containsExactlyInAnyOrder("MANUAL", "");
        assertThat(storeSalesRankRepository.findAll())
                .extracting(RestStopStoreSalesRankEntity::getRestStopServiceAreaCode)
                .containsExactlyInAnyOrder("MANUAL_STORE", "");
    }

    @Test
    @DisplayName("신규 휴게소가 추가된 뒤 다시 backfill하면 기존 미매핑 판매순위를 연결한다")
    void backfill_mapsPreviouslyUnmappedRankAfterRestStopIsAdded() {
        RestStopProductSalesRankEntity product = RestStopProductSalesRankEntity.from(
                new SalesRankingProductRow("2026-06", "1", "S000001", "신규휴게소", "M1", "매장", "P1", "상품"));
        productSalesRankRepository.save(product);

        Map<String, Integer> firstResult = backfillService.backfill();
        assertThat(firstResult.get(RestStopServiceAreaCodeBackfillService.PRODUCT_SALES_RANK_MAPPED_COUNT))
                .isZero();

        restStopRepository.save(RestStopEntity.from(restStopItem("001", "신규휴게소", "A00001")));

        Map<String, Integer> secondResult = backfillService.backfill();

        assertThat(secondResult.get(RestStopServiceAreaCodeBackfillService.PRODUCT_SALES_RANK_MAPPED_COUNT))
                .isEqualTo(1);
        assertThat(productSalesRankRepository.findAll().get(0).getRestStopServiceAreaCode())
                .isEqualTo("A00001");
    }

    private RestFoodEntity foodEntity(String stdRestCd, String foodName) throws Exception {
        String json = """
                {"stdRestCd":"%s","foodNm":"%s","foodCost":"7000","recommendyn":"N"}
                """.formatted(stdRestCd, foodName);
        RestBestfoodItem item = new ObjectMapper().readValue(json, RestBestfoodItem.class);
        return RestFoodEntity.from(item);
    }

    private EvChargerEntity chargerEntity(String statId, String statName, String latitude, String longitude)
            throws Exception {
        String json = "{\"statId\":\""
                + statId
                + "\",\"statNm\":\""
                + statName
                + "\",\"lat\":\""
                + latitude
                + "\",\"lng\":\""
                + longitude
                + "\",\"delYn\":\"N\"}";
        return EvChargerEntity.from(new ObjectMapper().readValue(json, EvChargerItem.class));
    }
}
