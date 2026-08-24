package com.restroute.oilprice.service;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static com.restroute.support.RestStopTestFixtures.restOilPriceResponse;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.common.client.ExApiClient;
import com.restroute.oilprice.client.response.RestOilPriceItem;
import com.restroute.oilprice.controller.response.OilInfoResponse;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.repository.RestOilPriceRepository;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.RestStopRelatedInfoQueryService;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class RestOilPriceRefreshServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    @Mock
    private RestOilPriceRepository restOilPriceRepository;

    @Mock
    private ExApiClient exApiClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T22:30:00Z"), ZoneId.of("Asia/Seoul"));

    private RestOilPriceRefreshService restOilPriceRefreshService;

    @BeforeEach
    void setUp() {
        restOilPriceRefreshService = new RestOilPriceRefreshService(
                restStopRepository,
                restStopRelatedInfoQueryService,
                restOilPriceRepository,
                exApiClient,
                transactionTemplate,
                clock);
    }

    @Test
    @DisplayName("휴게소 기준으로 주유소 가격을 실시간 조회해 기존 가격 row를 갱신한다")
    void refreshRestOilPrice_updatesExistingRow() {
        runTransactionCallback();
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity existing = RestOilPriceEntity.from(
                restOilPriceItem("000002", "서울만남(부산)주유소"), LocalDateTime.of(2026, 6, 16, 7, 10));
        RestOilPriceItem changed = restOilPriceItem("000002", "서울만남(부산)주유소");
        ReflectionTestUtils.setField(changed, "gasolinePrice", "1,888원");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(relatedInfo("000002", List.of(convenience), Optional.of(existing)));
        when(exApiClient.getCurStateStationByServiceAreaCode2("000002"))
                .thenReturn(restOilPriceResponse("SUCCESS", List.of(changed)));
        when(restOilPriceRepository.findByServiceAreaCode2("000002")).thenReturn(Optional.of(existing));

        Optional<OilInfoResponse> result = restOilPriceRefreshService.refreshByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().gasolinePrice()).isEqualTo("1,888원");
        assertThat(result.get().oilStationConveniences()).hasSize(1);
        assertThat(existing.getLastRefreshedAt()).isEqualTo(LocalDateTime.of(2026, 6, 16, 7, 30));
        assertThat(existing.getRestStopServiceAreaCode()).isEqualTo("A00001");
        verify(restOilPriceRepository, never()).save(any());
    }

    @Test
    @DisplayName("최근 10분 이내 갱신된 가격 row가 있으면 외부 API 호출 없이 DB 값을 반환한다")
    void refreshRestOilPrice_returnsCachedRowWhenFresh() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity cached = RestOilPriceEntity.from(
                restOilPriceItem("000002", "서울만남(부산)주유소"), LocalDateTime.of(2026, 6, 16, 7, 25));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(relatedInfo("000002", List.of(convenience), Optional.of(cached)));

        Optional<OilInfoResponse> result = restOilPriceRefreshService.refreshByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().gasolinePrice()).isEqualTo("1,999원");
        verify(exApiClient, never()).getCurStateStationByServiceAreaCode2(any());
    }

    @Test
    @DisplayName("저장된 가격 row가 없으면 새 가격 row를 저장한다")
    void refreshRestOilPrice_insertsWhenPriceMissing() {
        runTransactionCallback();
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilPriceItem fetched = restOilPriceItem("000002", "서울만남(부산)주유소");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(relatedInfo("000002", List.of(convenience), Optional.empty()));
        when(exApiClient.getCurStateStationByServiceAreaCode2("000002"))
                .thenReturn(restOilPriceResponse("SUCCESS", List.of(fetched)));
        when(restOilPriceRepository.findByServiceAreaCode2("000002")).thenReturn(Optional.empty());
        when(restOilPriceRepository.save(any(RestOilPriceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<OilInfoResponse> result = restOilPriceRefreshService.refreshByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().gasolinePrice()).isEqualTo("1,999원");
        ArgumentCaptor<RestOilPriceEntity> oilPriceCaptor = ArgumentCaptor.forClass(RestOilPriceEntity.class);
        verify(restOilPriceRepository).save(oilPriceCaptor.capture());
        assertThat(oilPriceCaptor.getValue().getRestStopServiceAreaCode()).isEqualTo("A00001");
    }

    @Test
    @DisplayName("갱신 시각이 없으면 외부 API를 호출해 갱신한다")
    void refreshRestOilPrice_refreshesWhenCachedRowHasNoTimestamp() {
        runTransactionCallback();
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity existing = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceItem changed = restOilPriceItem("000002", "서울만남(부산)주유소");
        ReflectionTestUtils.setField(changed, "gasolinePrice", "1,777원");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(relatedInfo("000002", List.of(convenience), Optional.of(existing)));
        when(exApiClient.getCurStateStationByServiceAreaCode2("000002"))
                .thenReturn(restOilPriceResponse("SUCCESS", List.of(changed)));
        when(restOilPriceRepository.findByServiceAreaCode2("000002")).thenReturn(Optional.of(existing));

        Optional<OilInfoResponse> result = restOilPriceRefreshService.refreshByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().gasolinePrice()).isEqualTo("1,777원");
    }

    @Test
    @DisplayName("휴게소가 없으면 단건 가격 갱신 대상이 없다")
    void refreshRestOilPrice_returnsEmptyWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<OilInfoResponse> result = restOilPriceRefreshService.refreshByServiceAreaCode("UNKNOWN");

        assertThat(result).isEmpty();
        verify(exApiClient, never()).getCurStateStationByServiceAreaCode2(any());
    }

    @Test
    @DisplayName("주유소 매핑이 없으면 단건 가격 갱신 대상이 없다")
    void refreshRestOilPrice_returnsEmptyWhenOilMappingMissing() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(relatedInfo(null, List.of(), Optional.empty()));

        Optional<OilInfoResponse> result = restOilPriceRefreshService.refreshByServiceAreaCode("A00001");

        assertThat(result).isEmpty();
        verify(exApiClient, never()).getCurStateStationByServiceAreaCode2(any());
    }

    @Test
    @DisplayName("upstream 단건 가격 결과가 없으면 DB를 변경하지 않는다")
    void refreshRestOilPrice_returnsEmptyWhenUpstreamResultMissing() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(relatedInfo("000002", List.of(convenience), Optional.empty()));
        when(exApiClient.getCurStateStationByServiceAreaCode2("000002"))
                .thenReturn(restOilPriceResponse("SUCCESS", List.of()));

        Optional<OilInfoResponse> result = restOilPriceRefreshService.refreshByServiceAreaCode("A00001");

        assertThat(result).isEmpty();
        verify(restOilPriceRepository, never()).save(any());
    }

    @Test
    @DisplayName("upstream 단건 가격 list가 null이면 DB를 변경하지 않는다")
    void refreshRestOilPrice_returnsEmptyWhenUpstreamListIsNull() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        var response = restOilPriceResponse("SUCCESS", List.of());
        ReflectionTestUtils.setField(response, "list", null);
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(relatedInfo("000002", List.of(convenience), Optional.empty()));
        when(exApiClient.getCurStateStationByServiceAreaCode2("000002")).thenReturn(response);

        Optional<OilInfoResponse> result = restOilPriceRefreshService.refreshByServiceAreaCode("A00001");

        assertThat(result).isEmpty();
        verify(restOilPriceRepository, never()).save(any());
    }

    private void runTransactionCallback() {
        doAnswer(invocation -> {
                    TransactionCallback<?> action = invocation.getArgument(0);
                    return action.doInTransaction(
                            org.mockito.Mockito.mock(org.springframework.transaction.TransactionStatus.class));
                })
                .when(transactionTemplate)
                .execute(any());
    }

    private RestStopRelatedInfo relatedInfo(
            String serviceAreaCode2,
            List<RestOilEntity> oilStationConveniences,
            Optional<RestOilPriceEntity> oilPrice) {
        return RestStopRelatedInfo.of(
                Optional.empty(),
                List.of(),
                oilStationConveniences,
                Optional.ofNullable(serviceAreaCode2),
                oilPrice,
                List.of(),
                List.of(),
                List.of());
    }
}
