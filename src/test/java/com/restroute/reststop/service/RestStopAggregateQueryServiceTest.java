package com.restroute.reststop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopRestroomEntity;
import com.restroute.reststop.domain.RestStopUsageSnapshotEntity;
import com.restroute.reststop.domain.SizeTier;
import com.restroute.reststop.repository.RestStopRestroomRepository;
import com.restroute.reststop.repository.RestStopUsageSnapshotRepository;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.reststop.service.image.RestStopImageQueryService;
import com.restroute.reststop.service.restroom.dto.RestStopRestroomRow;
import com.restroute.reststop.service.usage.dto.RestStopUsageSnapshotRow;
import com.restroute.reststopcontent.service.RestStopEventQueryService;
import com.restroute.reststopcontent.service.RestThemeQueryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopAggregateQueryServiceTest {

    @Mock
    private RestStopQueryService restStopQueryService;

    @Mock
    private RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    @Mock
    private EvChargerQueryService evChargerQueryService;

    @Mock
    private RestStopImageQueryService restStopImageQueryService;

    @Mock
    private RestThemeQueryService restThemeQueryService;

    @Mock
    private RestStopEventQueryService restStopEventQueryService;

    @Mock
    private RestStopRestroomRepository restStopRestroomRepository;

    @Mock
    private RestStopUsageSnapshotRepository restStopUsageSnapshotRepository;

    private RestStopAggregateQueryService aggregateQueryService;

    @BeforeEach
    void setUp() {
        aggregateQueryService = new RestStopAggregateQueryService(
                restStopQueryService,
                restStopRelatedInfoQueryService,
                evChargerQueryService,
                restStopImageQueryService,
                restThemeQueryService,
                restStopEventQueryService,
                restStopRestroomRepository,
                restStopUsageSnapshotRepository);
        lenient()
                .when(restStopRelatedInfoQueryService.findAllByRestStops(any(), any()))
                .thenReturn(Map.of());
        lenient()
                .when(evChargerQueryService.findChargerMappedServiceAreaCodes(any()))
                .thenReturn(List.of());
        lenient()
                .when(restStopImageQueryService.findExistingServiceAreaCodes(any()))
                .thenReturn(Set.of());
        lenient()
                .when(restThemeQueryService.findThemeMappedServiceAreaCodes(any()))
                .thenReturn(List.of());
        lenient()
                .when(restStopEventQueryService.findActiveEventMappedServiceAreaCodes(any()))
                .thenReturn(List.of());
        lenient()
                .when(restStopRestroomRepository.findAllByRestStopServiceAreaCodeIn(any()))
                .thenReturn(List.of());
        lenient()
                .when(restStopUsageSnapshotRepository.findAllByRestStopServiceAreaCodeIn(any()))
                .thenReturn(List.of());
    }

    private RestStopEntity restStop(String code) {
        RestStopEntity entity = mock(RestStopEntity.class);
        lenient().when(entity.getServiceAreaCode()).thenReturn(code);
        return entity;
    }

    @Test
    @DisplayName("코드 목록과 override 조건을 그대로 RestStopQueryService에 위임해 대상 휴게소를 조회한다")
    void find_delegatesResolutionToRestStopQueryService() {
        RestStopEntity restStop = restStop("A00001");
        when(restStopQueryService.findByServiceAreaCodesAndAdminOverridden(Set.of("A00001"), null))
                .thenReturn(List.of(restStop));

        Map<String, RestStopAggregate> result =
                aggregateQueryService.findByServiceAreaCodesAndAdminOverridden(Set.of("A00001"), null);

        assertThat(result).containsOnlyKeys("A00001");
        assertThat(result.get("A00001").restStop()).isEqualTo(restStop);
    }

    @Test
    @DisplayName("대상이 없으면 빈 맵을 반환하고 나머지 조회는 하지 않는다")
    void find_returnsEmptyMapWhenNoRestStops() {
        when(restStopQueryService.findByServiceAreaCodesAndAdminOverridden(any(), any()))
                .thenReturn(List.of());

        Map<String, RestStopAggregate> result =
                aggregateQueryService.findByServiceAreaCodesAndAdminOverridden(Set.of("UNKNOWN"), null);

        assertThat(result).isEmpty();
        verify(restStopRelatedInfoQueryService, org.mockito.Mockito.never()).findAllByRestStops(any(), any());
    }

    @Test
    @DisplayName("연관 정보, EV차저/이미지/테마/이벤트 매핑 여부를 코드별로 합쳐서 반환한다")
    void find_combinesRelatedInfoAndMappingFlagsPerCode() {
        RestStopEntity withEverything = restStop("A00001");
        RestStopEntity withNothing = restStop("A00002");
        when(restStopQueryService.findByServiceAreaCodesAndAdminOverridden(any(), any()))
                .thenReturn(List.of(withEverything, withNothing));

        RestStopRelatedInfo relatedInfo = RestStopRelatedInfo.of(
                Optional.empty(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of(),
                List.of());
        Map<String, RestStopRelatedInfo> relatedInfoByCode = new HashMap<>();
        relatedInfoByCode.put("A00001", relatedInfo);
        when(restStopRelatedInfoQueryService.findAllByRestStops(any(), any())).thenReturn(relatedInfoByCode);
        when(evChargerQueryService.findChargerMappedServiceAreaCodes(any())).thenReturn(List.of("A00001"));
        when(restStopImageQueryService.findExistingServiceAreaCodes(any())).thenReturn(Set.of("A00001"));
        when(restThemeQueryService.findThemeMappedServiceAreaCodes(any())).thenReturn(List.of("A00001"));
        when(restStopEventQueryService.findActiveEventMappedServiceAreaCodes(any()))
                .thenReturn(List.of("A00001"));

        Map<String, RestStopAggregate> result =
                aggregateQueryService.findByServiceAreaCodesAndAdminOverridden(null, null);

        RestStopAggregate first = result.get("A00001");
        assertThat(first.relatedInfo()).isEqualTo(relatedInfo);
        assertThat(first.hasEvCharger()).isTrue();
        assertThat(first.hasListImage()).isTrue();
        assertThat(first.hasTheme()).isTrue();
        assertThat(first.hasEvent()).isTrue();

        RestStopAggregate second = result.get("A00002");
        assertThat(second.relatedInfo()).isNull();
        assertThat(second.hasEvCharger()).isFalse();
        assertThat(second.hasListImage()).isFalse();
        assertThat(second.hasTheme()).isFalse();
        assertThat(second.hasEvent()).isFalse();
    }

    @Test
    @DisplayName("화장실 남/여 변기 수와 이용량 상위 10% 여부를 코드별로 합쳐서 반환하고, 매핑이 없으면 각각 null/false로 채운다")
    void find_combinesRestroomCountsAndTopTrafficTierPerCode() {
        RestStopEntity withUsageData = restStop("A00001");
        RestStopEntity withoutUsageData = restStop("A00002");
        when(restStopQueryService.findByServiceAreaCodesAndAdminOverridden(any(), any()))
                .thenReturn(List.of(withUsageData, withoutUsageData));

        RestStopRestroomEntity restroom =
                RestStopRestroomEntity.from(new RestStopRestroomRow("경부선", "A휴게소", "10", "8"));
        restroom.updateRestStopServiceAreaCode("A00001");
        when(restStopRestroomRepository.findAllByRestStopServiceAreaCodeIn(any()))
                .thenReturn(List.of(restroom));

        RestStopUsageSnapshotEntity usageSnapshot = RestStopUsageSnapshotEntity.from(
                new RestStopUsageSnapshotRow("경부선", "A휴게소", "1000", "휴게소", "500", "3000"));
        usageSnapshot.updateRestStopServiceAreaCode("A00001");
        usageSnapshot.updateTopTrafficTier(true);
        when(restStopUsageSnapshotRepository.findAllByRestStopServiceAreaCodeIn(any()))
                .thenReturn(List.of(usageSnapshot));

        Map<String, RestStopAggregate> result =
                aggregateQueryService.findByServiceAreaCodesAndAdminOverridden(null, null);

        RestStopAggregate first = result.get("A00001");
        assertThat(first.maleToiletCount()).isEqualTo(10);
        assertThat(first.femaleToiletCount()).isEqualTo(8);
        assertThat(first.topTrafficTier()).isTrue();

        RestStopAggregate second = result.get("A00002");
        assertThat(second.maleToiletCount()).isNull();
        assertThat(second.femaleToiletCount()).isNull();
        assertThat(second.topTrafficTier()).isFalse();
    }

    @Test
    @DisplayName("부지면적 규모 등급을 코드별로 합쳐서 반환하고, 매핑이 없으면 null로 채운다")
    void find_combinesSizeTierPerCode() {
        RestStopEntity withUsageData = restStop("A00001");
        RestStopEntity withoutUsageData = restStop("A00002");
        when(restStopQueryService.findByServiceAreaCodesAndAdminOverridden(any(), any()))
                .thenReturn(List.of(withUsageData, withoutUsageData));

        RestStopUsageSnapshotEntity usageSnapshot = RestStopUsageSnapshotEntity.from(
                new RestStopUsageSnapshotRow("경부선", "A휴게소", "1000", "휴게소", "500", "3000"));
        usageSnapshot.updateRestStopServiceAreaCode("A00001");
        usageSnapshot.updateSizeTier(SizeTier.LARGE);
        when(restStopUsageSnapshotRepository.findAllByRestStopServiceAreaCodeIn(any()))
                .thenReturn(List.of(usageSnapshot));

        Map<String, RestStopAggregate> result =
                aggregateQueryService.findByServiceAreaCodesAndAdminOverridden(null, null);

        assertThat(result.get("A00001").sizeTier()).isEqualTo(SizeTier.LARGE);
        assertThat(result.get("A00002").sizeTier()).isNull();
    }

    @Test
    @DisplayName("같은 서비스지역코드로 휴게소가 중복 조회돼도 예외 없이 첫 번째 값을 사용한다")
    void find_keepsFirstRestStopWhenDuplicateServiceAreaCodeExists() {
        RestStopEntity first = restStop("A00001");
        RestStopEntity duplicate = restStop("A00001");
        when(restStopQueryService.findByServiceAreaCodesAndAdminOverridden(any(), any()))
                .thenReturn(List.of(first, duplicate));

        Map<String, RestStopAggregate> result =
                aggregateQueryService.findByServiceAreaCodesAndAdminOverridden(Set.of("A00001"), null);

        assertThat(result).containsOnlyKeys("A00001");
        assertThat(result.get("A00001").restStop()).isEqualTo(first);
    }

    @Test
    @DisplayName("코드 없이 override=false만 넘기면(backfill 용도) 그대로 RestStopQueryService에 전달한다")
    void find_passesNullCodesAndOverriddenFalseThroughForBackfillUseCase() {
        when(restStopQueryService.findByServiceAreaCodesAndAdminOverridden(null, false))
                .thenReturn(List.of());

        aggregateQueryService.findByServiceAreaCodesAndAdminOverridden(null, false);

        verify(restStopQueryService).findByServiceAreaCodesAndAdminOverridden(null, false);
    }

    @Test
    @DisplayName("미리 조회해둔 RestStopEntity 목록을 받으면 RestStopQueryService를 다시 호출하지 않는다")
    void findByRestStops_doesNotQueryRestStopsAgain() {
        RestStopEntity restStop = restStop("A00001");

        Map<String, RestStopAggregate> result =
                aggregateQueryService.findByRestStopsAndAdminOverridden(List.of(restStop), null);

        assertThat(result).containsOnlyKeys("A00001");
        assertThat(result.get("A00001").restStop()).isEqualTo(restStop);
        verify(restStopQueryService, org.mockito.Mockito.never())
                .findByServiceAreaCodesAndAdminOverridden(any(), any());
    }

    @Test
    @DisplayName("미리 조회해둔 목록이 비어있으면 빈 맵을 반환하고 나머지 조회는 하지 않는다")
    void findByRestStops_returnsEmptyMapWhenGivenListIsEmpty() {
        Map<String, RestStopAggregate> result =
                aggregateQueryService.findByRestStopsAndAdminOverridden(List.of(), null);

        assertThat(result).isEmpty();
        verify(restStopRelatedInfoQueryService, org.mockito.Mockito.never()).findAllByRestStops(any(), any());
    }
}
