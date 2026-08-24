package com.restroute.reststop.service;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.restroute.reststop.controller.response.RestStopDetailViewResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopQueryServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    private RestStopQueryService restStopQueryService;

    @BeforeEach
    void setUp() {
        restStopQueryService = new RestStopQueryService(restStopRepository);
    }

    @Test
    @DisplayName("저장된 휴게소 목록을 조회한다")
    void findAll_returnsSavedRestStops() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));

        List<RestStopEntity> restStops = restStopQueryService.findAll();

        assertThat(restStops).containsExactly(restStop);
    }

    @Test
    @DisplayName("serviceAreaCode 기준으로 조회한 휴게소 상세 진입용 최소 응답을 반환한다")
    void findDetailByServiceAreaCode_returnsMinimalDetail() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));

        Optional<RestStopDetailViewResponse> result = restStopQueryService.findDetailByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        RestStopDetailViewResponse response = result.get();
        assertThat(response.serviceAreaCode()).isEqualTo("A00001");
        assertThat(response.unitCode()).isEqualTo("001");
        assertThat(response.unitName()).isEqualTo("서울만남(부산)휴게소");
        assertThat(response.routeNo()).isEqualTo("0010");
        assertThat(response.routeName()).isEqualTo("경부선");
        assertThat(response.xValue()).isEqualTo("127.042514");
        assertThat(response.yValue()).isEqualTo("37.459939");
        assertThat(response.stdRestCd()).isEqualTo("000001");
    }

    @Test
    @DisplayName("기준 휴게소가 없으면 상세 정보를 반환하지 않는다")
    void findDetailByServiceAreaCode_returnsEmptyWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<RestStopDetailViewResponse> result = restStopQueryService.findDetailByServiceAreaCode("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("휴게소명으로 검색하면 레포지토리 조회 결과를 그대로 반환한다")
    void searchByName_delegatesToRepository() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByUnitNameContainingIgnoreCase("서울만남")).thenReturn(List.of(restStop));

        List<RestStopEntity> results = restStopQueryService.searchByName("서울만남");

        assertThat(results).containsExactly(restStop);
    }

    @Test
    @DisplayName("공백뿐인 검색어는 레포지토리를 조회하지 않고 빈 목록을 반환한다")
    void searchByName_returnsEmptyForBlankQuery() {
        List<RestStopEntity> results = restStopQueryService.searchByName("   ");

        assertThat(results).isEmpty();
        verifyNoInteractions(restStopRepository);
    }

    @Test
    @DisplayName("코드 목록이 주어지면 그 코드와 override 조건을 그대로 레포지토리에 위임한다")
    void findByServiceAreaCodesAndAdminOverridden_delegatesCodesAsIs() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCodesAndAdminOverridden(Set.of("A00001"), null))
                .thenReturn(List.of(restStop));

        List<RestStopEntity> results =
                restStopQueryService.findByServiceAreaCodesAndAdminOverridden(Set.of("A00001"), null);

        assertThat(results).containsExactly(restStop);
    }

    @Test
    @DisplayName("코드 목록이 null이면 코드 필터 없이 레포지토리에 null을 전달한다")
    void findByServiceAreaCodesAndAdminOverridden_passesNullCodesThrough() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCodesAndAdminOverridden(null, false))
                .thenReturn(List.of(restStop));

        List<RestStopEntity> results = restStopQueryService.findByServiceAreaCodesAndAdminOverridden(null, false);

        assertThat(results).containsExactly(restStop);
    }

    @Test
    @DisplayName("코드 목록이 빈 컬렉션이면 null로 정규화해 레포지토리에 전달한다")
    void findByServiceAreaCodesAndAdminOverridden_normalizesEmptyCodesToNull() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCodesAndAdminOverridden(null, null))
                .thenReturn(List.of(restStop));

        List<RestStopEntity> results = restStopQueryService.findByServiceAreaCodesAndAdminOverridden(Set.of(), null);

        assertThat(results).containsExactly(restStop);
    }
}
