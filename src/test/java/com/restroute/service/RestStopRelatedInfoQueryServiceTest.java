package com.restroute.service;

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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.RestBestfoodItem;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestEventEntity;
import com.restroute.domain.RestFoodEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.domain.RestThemeEntity;
import com.restroute.repository.HighwayServiceAreaInfoRepository;
import com.restroute.repository.RestEventRepository;
import com.restroute.repository.RestFoodRepository;
import com.restroute.repository.RestOilPriceRepository;
import com.restroute.repository.RestOilRepository;
import com.restroute.repository.RestStopDetailRepository;
import com.restroute.repository.RestThemeRepository;
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

    private RestFoodEntity foodEntity(String foodName) throws Exception {
        RestBestfoodItem item = new ObjectMapper()
                .readValue("{\"stdRestCd\":\"000001\",\"foodNm\":\"" + foodName + "\"}", RestBestfoodItem.class);
        ReflectionTestUtils.setField(item, "recommendyn", "Y");
        return RestFoodEntity.from(item);
    }
}
