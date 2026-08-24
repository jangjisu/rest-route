package com.restroute.evcharger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.evcharger.client.EvChargerApiClient;
import com.restroute.evcharger.client.response.EvChargerResponse;
import com.restroute.evcharger.domain.EvChargerEntity;
import com.restroute.evcharger.repository.EvChargerRepository;
import com.restroute.evcharger.service.dto.EvChargerSyncResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class EvChargerSyncServiceTest {

    @Mock
    private EvChargerApiClient evChargerApiClient;

    @Mock
    private EvChargerRepository evChargerRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private EvChargerSyncService evChargerSyncService;

    @BeforeEach
    void setUp() {
        evChargerSyncService = new EvChargerSyncService(evChargerApiClient, evChargerRepository, transactionTemplate);
    }

    @Test
    @DisplayName("전체 페이지를 조회하고 C001 고속도로 휴게소 충전기만 upsert한다")
    void refreshEvChargers_fetchesAllPagesAndFiltersHighwayRestStops() throws Exception {
        runTransactionCallback();
        when(evChargerRepository.findAll()).thenReturn(List.of());
        when(evChargerApiClient.getChargerInfo(1))
                .thenReturn(response(
                        401, 1, charger("ME1", "01", "C001"), charger("", "01", "C001"), charger("ME4", "", "C001")));
        when(evChargerApiClient.getChargerInfo(2)).thenReturn(response(401, 2, charger("ME2", "01", "C003")));

        EvChargerSyncResult result = evChargerSyncService.refreshEvChargers();

        assertThat(result.savedItemCount()).isEqualTo(1);
        assertThat(result.totalPageCount()).isEqualTo(3);
        assertThat(captureSavedEntities())
                .extracting(EvChargerEntity::getStatId)
                .containsExactly("ME1");
        verify(evChargerApiClient).getChargerInfo(2);
    }

    @Test
    @DisplayName("같은 statId와 chgerId가 있으면 기존 충전기 행을 업데이트한다")
    void refreshEvChargers_updatesExistingNaturalKey() throws Exception {
        runTransactionCallback();
        EvChargerEntity existing = chargerEntity("ME1", "01", "C001", "existing");
        when(evChargerRepository.findAll()).thenReturn(List.of(existing));
        when(evChargerApiClient.getChargerInfo(1)).thenReturn(response(1, 1, charger("ME1", "01", "C001", "updated")));

        evChargerSyncService.refreshEvChargers();

        assertThat(captureSavedEntities()).singleElement().satisfies(entity -> {
            assertThat(entity).isSameAs(existing);
            assertThat(entity.getStatNm()).isEqualTo("updated");
        });
    }

    @Test
    @DisplayName("중간 페이지 실패 시 성공한 페이지를 저장하고 다음 페이지를 계속 조회한다")
    void refreshEvChargers_savesSuccessfulPagesAndContinuesAfterPageFailure() throws Exception {
        runTransactionCallback();
        when(evChargerRepository.findAll()).thenReturn(List.of());
        when(evChargerApiClient.getChargerInfo(1)).thenReturn(response(401, 1, charger("ME1", "01", "C001")));
        RuntimeException exception = new RuntimeException("timeout");
        when(evChargerApiClient.getChargerInfo(2)).thenThrow(exception);
        when(evChargerApiClient.getChargerInfo(3)).thenReturn(response(401, 3, charger("ME3", "01", "C001")));

        EvChargerSyncResult result = evChargerSyncService.refreshEvChargers();

        assertThat(result.failedPageCount()).isEqualTo(1);
        assertThat(result.successfulPageCount()).isEqualTo(2);
        assertThat(captureSavedEntities())
                .extracting(EvChargerEntity::getStatId)
                .containsExactly("ME1", "ME3");
        verify(evChargerApiClient).getChargerInfo(3);
    }

    @Test
    @DisplayName("첫 페이지 실패 시 추가 페이지를 조회하지 않고 기존 데이터를 유지한다")
    void refreshEvChargers_keepsExistingDataWhenFirstPageFails() {
        RuntimeException exception = new RuntimeException("timeout");
        when(evChargerApiClient.getChargerInfo(1)).thenThrow(exception);

        EvChargerSyncResult result = evChargerSyncService.refreshEvChargers();

        assertThat(result.successfulPageCount()).isZero();
        assertThat(result.failedPageCount()).isEqualTo(1);
        verify(evChargerApiClient, never()).getChargerInfo(2);
        verify(evChargerRepository, never()).findAll();
        verify(evChargerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("충전기 테이블에 데이터가 있으면 초기 동기화를 생략한다")
    void initializeEvChargersIfEmpty_skipsWhenDataExists() {
        when(evChargerRepository.count()).thenReturn(1L);

        EvChargerSyncResult result = evChargerSyncService.initializeEvChargersIfEmpty();

        assertThat(result).isEqualTo(EvChargerSyncResult.skipped());
        verify(evChargerApiClient, never()).getChargerInfo(1);
    }

    private EvChargerResponse response(int totalCount, int pageNo, String... items) throws Exception {
        String json = "{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL SERVICE.\",\"totalCount\":"
                + totalCount
                + ",\"pageNo\":"
                + pageNo
                + ",\"numOfRows\":200,\"items\":{\"item\":["
                + String.join(",", items)
                + "]}}";
        return new ObjectMapper().readValue(json, EvChargerResponse.class);
    }

    private String charger(String statId, String chgerId, String kindDetail) {
        return charger(statId, chgerId, kindDetail, "station");
    }

    private String charger(String statId, String chgerId, String kindDetail, String statName) {
        return "{\"statNm\":\""
                + statName
                + "\",\"statId\":\""
                + statId
                + "\",\"chgerId\":\""
                + chgerId
                + "\",\"kindDetail\":\""
                + kindDetail
                + "\",\"delYn\":\"N\"}";
    }

    private EvChargerEntity chargerEntity(String statId, String chgerId, String kindDetail, String statName)
            throws Exception {
        return EvChargerEntity.from(new ObjectMapper()
                .readValue(
                        charger(statId, chgerId, kindDetail, statName),
                        com.restroute.evcharger.client.response.EvChargerItem.class));
    }

    private void runTransactionCallback() {
        doAnswer(invocation -> {
                    Consumer<TransactionStatus> action = invocation.getArgument(0);
                    action.accept(org.mockito.Mockito.mock(TransactionStatus.class));
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());
    }

    @SuppressWarnings("unchecked")
    private List<EvChargerEntity> captureSavedEntities() {
        ArgumentCaptor<Iterable<EvChargerEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(evChargerRepository).saveAll(captor.capture());
        List<EvChargerEntity> entities = new ArrayList<>();
        captor.getValue().forEach(entities::add);
        return entities;
    }
}
