package com.restroute.reststop.service;

import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoItem;
import static com.restroute.support.RestStopTestFixtures.restEventItem;
import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static com.restroute.support.RestStopTestFixtures.restThemeItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.repository.RestOilPriceRepository;
import com.restroute.oilprice.repository.RestOilRepository;
import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.HighwayServiceAreaInfoRepository;
import com.restroute.reststop.repository.RestStopDetailRepository;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.reststopcontent.client.response.RestBestfoodItem;
import com.restroute.reststopcontent.domain.RestEventEntity;
import com.restroute.reststopcontent.domain.RestFoodEntity;
import com.restroute.reststopcontent.domain.RestThemeEntity;
import com.restroute.reststopcontent.repository.RestEventRepository;
import com.restroute.reststopcontent.repository.RestFoodRepository;
import com.restroute.reststopcontent.repository.RestThemeRepository;
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
class RestStopRelatedInfoQueryServiceTest {

    @Mock
    private RestStopDetailRepository restStopDetailRepository;

    @Mock
    private HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;

    @Mock
    private RestOilRepository restOilRepository;

    @Mock
    private RestOilPriceRepository restOilPriceRepository;

    @Mock
    private RestFoodRepository restFoodRepository;

    @Mock
    private RestThemeRepository restThemeRepository;

    @Mock
    private RestEventRepository restEventRepository;

    private RestStopRelatedInfoQueryService service;

    @BeforeEach
    void setUp() {
        service = new RestStopRelatedInfoQueryService(
                restStopDetailRepository,
                highwayServiceAreaInfoRepository,
                restOilRepository,
                restOilPriceRepository,
                restFoodRepository,
                restThemeRepository,
                restEventRepository);
    }

    @Test
    @DisplayName("rest_stop_service_area_code 기준으로 상세, 영업시설, 주유, 가격, 음식 정보를 우선 조회한다")
    void findByRestStop_returnsRelatedInfo() throws Exception {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        HighwayServiceAreaInfoEntity info =
                HighwayServiceAreaInfoEntity.from(highwayServiceAreaInfoItem("000001", "서울만남주유소"));
        RestOilEntity oilConvenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestFoodEntity food = foodEntity("농심어묵우동");
        RestThemeEntity theme = RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소"));
        RestEventEntity event = RestEventEntity.from(restEventItem("000001", "1665"));
        detail.updateRestStopServiceAreaCode("A00001");
        info.updateRestStopServiceAreaCode("A00001");
        oilConvenience.updateRestStopServiceAreaCode("A00001");
        oilPrice.updateRestStopServiceAreaCode("A00001");
        food.updateRestStopServiceAreaCode("A00001");
        theme.updateRestStopServiceAreaCode("A00001");
        event.updateRestStopServiceAreaCode("A00001");

        when(restStopDetailRepository.findByRestStopServiceAreaCode("A00001")).thenReturn(Optional.of(detail));
        when(highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCode("A00001"))
                .thenReturn(List.of(info));
        when(restOilRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of(oilConvenience));
        when(restOilPriceRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of(oilPrice));
        when(restFoodRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of(food));
        when(restThemeRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of(theme));
        when(restEventRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of(event));

        RestStopRelatedInfo relatedInfo = service.findByRestStop(restStop);

        assertThat(relatedInfo.detail()).contains(detail);
        assertThat(relatedInfo.highwayServiceAreaInfos()).containsExactly(info);
        assertThat(relatedInfo.oilStationConveniences()).containsExactly(oilConvenience);
        assertThat(relatedInfo.oilServiceAreaCode2()).contains("000002");
        assertThat(relatedInfo.oilPrice()).contains(oilPrice);
        assertThat(relatedInfo.foods()).containsExactly(food);
        assertThat(relatedInfo.themes()).containsExactly(theme);
        assertThat(relatedInfo.events()).containsExactly(event);
        verify(restStopDetailRepository, never()).findByServiceAreaCode(anyString());
        verify(highwayServiceAreaInfoRepository, never()).findAllByBusinessFacilityCode(anyString());
        verify(restOilRepository, never())
                .findAllByRouteCodeAndNormalizedStationNameOrderByIdAsc(anyString(), anyString());
        verify(restOilPriceRepository, never()).findByServiceAreaCode2(anyString());
        verify(restFoodRepository, never()).findAllByStdRestCdOrderByIdAsc(anyString());
    }

    @Test
    @DisplayName("새 조회 키 결과가 없으면 기존 원본 키로 fallback하지 않고 빈 관련 정보를 반환한다")
    void findByRestStop_doesNotFallBackToOriginalKeysWhenLookupKeyRowsMissing() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));

        when(restStopDetailRepository.findByRestStopServiceAreaCode("A00001")).thenReturn(Optional.empty());
        when(highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCode("A00001"))
                .thenReturn(List.of());
        when(restOilRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of());
        when(restFoodRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of());
        when(restThemeRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of());
        when(restEventRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of());

        RestStopRelatedInfo relatedInfo = service.findByRestStop(restStop);

        assertThat(relatedInfo.detail()).isEmpty();
        assertThat(relatedInfo.highwayServiceAreaInfos()).isEmpty();
        assertThat(relatedInfo.oilStationConveniences()).isEmpty();
        assertThat(relatedInfo.oilPrice()).isEmpty();
        assertThat(relatedInfo.foods()).isEmpty();
        assertThat(relatedInfo.themes()).isEmpty();
        assertThat(relatedInfo.events()).isEmpty();
        verify(restStopDetailRepository, never()).findByServiceAreaCode(anyString());
        verify(highwayServiceAreaInfoRepository, never()).findAllByBusinessFacilityCode(anyString());
        verify(restOilRepository, never())
                .findAllByRouteCodeAndNormalizedStationNameOrderByIdAsc(anyString(), anyString());
        verify(restOilPriceRepository, never()).findByServiceAreaCode2(anyString());
        verify(restFoodRepository, never()).findAllByStdRestCdOrderByIdAsc(anyString());
    }

    @Test
    @DisplayName("주유 편의시설 매핑이 없으면 주유 가격을 조회하지 않는다")
    void findByRestStop_skipsOilPriceWhenOilMappingMissing() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));

        when(restStopDetailRepository.findByRestStopServiceAreaCode("A00001")).thenReturn(Optional.empty());
        when(highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCode("A00001"))
                .thenReturn(List.of());
        when(restOilRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of());
        when(restFoodRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of());
        when(restThemeRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of());
        when(restEventRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of());

        RestStopRelatedInfo relatedInfo = service.findByRestStop(restStop);

        assertThat(relatedInfo.detail()).isEmpty();
        assertThat(relatedInfo.highwayServiceAreaInfos()).isEmpty();
        assertThat(relatedInfo.oilStationConveniences()).isEmpty();
        assertThat(relatedInfo.oilServiceAreaCode2()).isEmpty();
        assertThat(relatedInfo.oilPrice()).isEmpty();
        assertThat(relatedInfo.foods()).isEmpty();
        assertThat(relatedInfo.themes()).isEmpty();
        assertThat(relatedInfo.events()).isEmpty();
        verify(restStopDetailRepository, never()).findByServiceAreaCode(anyString());
        verify(highwayServiceAreaInfoRepository, never()).findAllByBusinessFacilityCode(anyString());
        verify(restOilRepository, never())
                .findAllByRouteCodeAndNormalizedStationNameOrderByIdAsc(anyString(), anyString());
        verify(restOilPriceRepository, never()).findByServiceAreaCode2(org.mockito.ArgumentMatchers.anyString());
        verify(restFoodRepository, never()).findAllByStdRestCdOrderByIdAsc(anyString());
    }

    @Test
    @DisplayName("여러 휴게소를 배치로 조회해 서비스지역코드별 관련 정보 맵으로 묶어 반환한다(N+1 방지)")
    void findAllByRestStops_batchesQueriesAcrossRestStops() throws Exception {
        RestStopEntity restStop1 = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopEntity restStop2 = RestStopEntity.from(restStopItem("002", "안성휴게소", "A00002"));

        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        detail.updateRestStopServiceAreaCode("A00001");
        HighwayServiceAreaInfoEntity info =
                HighwayServiceAreaInfoEntity.from(highwayServiceAreaInfoItem("000001", "서울만남주유소"));
        info.updateRestStopServiceAreaCode("A00001");
        RestOilEntity oilConvenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        oilConvenience.updateRestStopServiceAreaCode("A00001");
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        oilPrice.updateRestStopServiceAreaCode("A00001");
        RestFoodEntity food = foodEntity("농심어묵우동");
        food.updateRestStopServiceAreaCode("A00001");
        RestThemeEntity theme = RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소"));
        theme.updateRestStopServiceAreaCode("A00001");
        RestEventEntity event = RestEventEntity.from(restEventItem("000001", "1665"));
        event.updateRestStopServiceAreaCode("A00001");

        List<String> codes = List.of("A00001", "A00002");
        when(restStopDetailRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, null))
                .thenReturn(List.of(detail));
        when(highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCodeIn(codes))
                .thenReturn(List.of(info));
        when(restOilRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, null))
                .thenReturn(List.of(oilConvenience));
        when(restOilPriceRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of(oilPrice));
        when(restFoodRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, null))
                .thenReturn(List.of(food));
        when(restThemeRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of(theme));
        when(restEventRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of(event));

        Map<String, RestStopRelatedInfo> relatedInfoByCode =
                service.findAllByRestStops(List.of(restStop1, restStop2), null);

        RestStopRelatedInfo info1 = relatedInfoByCode.get("A00001");
        assertThat(info1.detail()).contains(detail);
        assertThat(info1.highwayServiceAreaInfos()).containsExactly(info);
        assertThat(info1.oilStationConveniences()).containsExactly(oilConvenience);
        assertThat(info1.oilServiceAreaCode2()).contains("000002");
        assertThat(info1.oilPrice()).contains(oilPrice);
        assertThat(info1.foods()).containsExactly(food);
        assertThat(info1.themes()).containsExactly(theme);
        assertThat(info1.events()).containsExactly(event);

        RestStopRelatedInfo info2 = relatedInfoByCode.get("A00002");
        assertThat(info2.detail()).isEmpty();
        assertThat(info2.highwayServiceAreaInfos()).isEmpty();
        assertThat(info2.oilStationConveniences()).isEmpty();
        assertThat(info2.oilServiceAreaCode2()).isEmpty();
        assertThat(info2.oilPrice()).isEmpty();
        assertThat(info2.foods()).isEmpty();
        assertThat(info2.themes()).isEmpty();
        assertThat(info2.events()).isEmpty();

        verify(restStopDetailRepository, never()).findByRestStopServiceAreaCode(anyString());
        verify(highwayServiceAreaInfoRepository, never()).findAllByRestStopServiceAreaCode(anyString());
        verify(restOilRepository, never()).findAllByRestStopServiceAreaCodeOrderByIdAsc(anyString());
        verify(restOilPriceRepository, never()).findAllByRestStopServiceAreaCodeOrderByIdAsc(anyString());
        verify(restFoodRepository, never()).findAllByRestStopServiceAreaCodeOrderByIdAsc(anyString());
        verify(restThemeRepository, never()).findAllByRestStopServiceAreaCodeOrderByIdAsc(anyString());
        verify(restEventRepository, never()).findAllByRestStopServiceAreaCodeOrderByIdAsc(anyString());
    }

    @Test
    @DisplayName("같은 서비스지역코드로 상세 정보가 중복 조회되어도 예외 없이 첫 번째 값을 사용한다")
    void findAllByRestStops_keepsFirstDetailWhenDuplicateServiceAreaCodeRowsExist() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopDetailEntity firstDetail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        firstDetail.updateRestStopServiceAreaCode("A00001");
        RestStopDetailEntity duplicateDetail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        duplicateDetail.updateRestStopServiceAreaCode("A00001");

        List<String> codes = List.of("A00001");
        when(restStopDetailRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, null))
                .thenReturn(List.of(firstDetail, duplicateDetail));
        when(highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCodeIn(codes))
                .thenReturn(List.of());
        when(restOilRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, null))
                .thenReturn(List.of());
        when(restOilPriceRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of());
        when(restFoodRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, null))
                .thenReturn(List.of());
        when(restThemeRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of());
        when(restEventRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of());

        Map<String, RestStopRelatedInfo> relatedInfoByCode = service.findAllByRestStops(List.of(restStop), null);

        assertThat(relatedInfoByCode.get("A00001").detail()).contains(firstDetail);
    }

    @Test
    @DisplayName("휴게소 목록이 비어 있으면 리포지토리를 호출하지 않고 빈 맵을 반환한다")
    void findAllByRestStops_returnsEmptyMapForEmptyInput() {
        Map<String, RestStopRelatedInfo> result = service.findAllByRestStops(List.of(), null);

        assertThat(result).isEmpty();
        verifyNoInteractions(
                restStopDetailRepository,
                highwayServiceAreaInfoRepository,
                restOilRepository,
                restOilPriceRepository,
                restFoodRepository,
                restThemeRepository,
                restEventRepository);
    }

    @Test
    @DisplayName("adminOverridden=false는 잠금 상태를 소유한 detail/oil/food에만 전달한다")
    void findAllByRestStops_passesAdminOverriddenFilterToSupportingRepositories() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        List<String> codes = List.of("A00001");
        when(restStopDetailRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, false))
                .thenReturn(List.of());
        when(highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCodeIn(codes))
                .thenReturn(List.of());
        when(restOilRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, false))
                .thenReturn(List.of());
        when(restOilPriceRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of());
        when(restFoodRepository.findByRestStopServiceAreaCodesAndAdminOverridden(codes, false))
                .thenReturn(List.of());
        when(restThemeRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of());
        when(restEventRepository.findAllByRestStopServiceAreaCodeIn(codes)).thenReturn(List.of());

        service.findAllByRestStops(List.of(restStop), false);

        verify(restStopDetailRepository).findByRestStopServiceAreaCodesAndAdminOverridden(codes, false);
        verify(restOilRepository).findByRestStopServiceAreaCodesAndAdminOverridden(codes, false);
        verify(restOilPriceRepository).findAllByRestStopServiceAreaCodeIn(codes);
        verify(restFoodRepository).findByRestStopServiceAreaCodesAndAdminOverridden(codes, false);
        verify(restThemeRepository).findAllByRestStopServiceAreaCodeIn(codes);
        verify(restEventRepository).findAllByRestStopServiceAreaCodeIn(codes);
    }

    private RestFoodEntity foodEntity(String foodName) throws Exception {
        RestBestfoodItem item = new ObjectMapper()
                .readValue("{\"stdRestCd\":\"000001\",\"foodNm\":\"" + foodName + "\"}", RestBestfoodItem.class);
        ReflectionTestUtils.setField(item, "recommendyn", "Y");
        return RestFoodEntity.from(item);
    }
}
