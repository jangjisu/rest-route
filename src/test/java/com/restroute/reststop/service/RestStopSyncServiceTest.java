package com.restroute.reststop.service;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static com.restroute.support.RestStopTestFixtures.restStopResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.client.ExApiClient;
import com.restroute.client.exception.ExApiException;
import com.restroute.reststop.client.response.RestStopItem;
import com.restroute.reststop.client.response.RestStopResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
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
class RestStopSyncServiceTest {

    @Mock
    private ExApiClient exApiClient;

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private RestStopSyncService restStopSyncService;

    @BeforeEach
    void setUp() {
        restStopSyncService = new RestStopSyncService(exApiClient, restStopRepository, transactionTemplate);
    }

    @Test
    @DisplayName("총 페이지 수만큼 휴게소 API를 호출하고, 기존에 없는 자연키는 새로 삽입한다")
    void refreshRestStops_fetchesAllPagesAndInsertsNewRows() {
        runTransactionCallback();
        when(restStopRepository.findAll()).thenReturn(List.of());
        RestStopItem first = restStopItem("001", "서울만남(부산)휴게소", "A00001");
        RestStopItem second = restStopItem("002", "죽전(서울)휴게소", "A00002");
        RestStopItem third = restStopItem("003", "기흥(부산)휴게소", "A00003");
        when(exApiClient.getLocationInfoRest(1)).thenReturn(restStopResponse("SUCCESS", "3", List.of(first)));
        when(exApiClient.getLocationInfoRest(2)).thenReturn(restStopResponse("SUCCESS", "3", List.of(second)));
        when(exApiClient.getLocationInfoRest(3)).thenReturn(restStopResponse("SUCCESS", "3", List.of(third)));

        int savedCount = restStopSyncService.refreshRestStops();

        assertThat(savedCount).isEqualTo(3);
        List<RestStopEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities).extracting(RestStopEntity::getUnitCode).containsExactly("001", "002", "003");
    }

    @Test
    @DisplayName("기존 DB에 같은 serviceAreaCode가 있으면 같은 행을 업데이트한다")
    void refreshRestStops_updatesExistingRowWithSameServiceAreaCode() {
        runTransactionCallback();
        RestStopItem originalItem = restStopItem("001", "서울만남(부산)휴게소");
        RestStopEntity existing = RestStopEntity.from(originalItem);
        when(restStopRepository.findAll()).thenReturn(List.of(existing));
        RestStopItem updatedItem = restStopItem("001", "이름이바뀐휴게소");
        when(exApiClient.getLocationInfoRest(1)).thenReturn(restStopResponse("SUCCESS", "1", List.of(updatedItem)));

        int savedCount = restStopSyncService.refreshRestStops();

        assertThat(savedCount).isEqualTo(1);
        List<RestStopEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities).hasSize(1);
        assertThat(savedEntities.get(0)).isSameAs(existing);
        assertThat(savedEntities.get(0).getUnitName()).isEqualTo("이름이바뀐휴게소");
    }

    @Test
    @DisplayName("같은 응답 안에 serviceAreaCode가 중복되면 한 행으로 합쳐 저장한다")
    void refreshRestStops_mergesDuplicateServiceAreaCodesWithinSameBatch() {
        runTransactionCallback();
        when(restStopRepository.findAll()).thenReturn(List.of());
        when(exApiClient.getLocationInfoRest(1)).thenReturn(duplicateServiceAreaCodeResponse());

        int savedCount = restStopSyncService.refreshRestStops();

        assertThat(savedCount).isEqualTo(2);
        List<RestStopEntity> saved = captureSavedEntities();
        List<RestStopEntity> distinctRows = saved.stream().distinct().toList();
        assertThat(distinctRows).hasSize(1);
        assertThat(distinctRows.get(0).getUnitName()).isEqualTo("이름이바뀐휴게소");
    }

    @Test
    @DisplayName("DB에 이미 같은 serviceAreaCode의 행이 두 개 있어도 예외 없이 첫 번째 행을 유지한다")
    void refreshRestStops_toleratesPreExistingDuplicateNaturalKeysInDb() {
        runTransactionCallback();
        RestStopEntity duplicate1 = RestStopEntity.from(restStopItem("001", "먼저", "A00001"));
        RestStopEntity duplicate2 = RestStopEntity.from(restStopItem("001", "나중", "A00001"));
        when(restStopRepository.findAll()).thenReturn(List.of(duplicate1, duplicate2));
        RestStopItem updatedItem = restStopItem("001", "업데이트됨", "A00001");
        when(exApiClient.getLocationInfoRest(1)).thenReturn(restStopResponse("SUCCESS", "1", List.of(updatedItem)));

        int savedCount = restStopSyncService.refreshRestStops();

        assertThat(savedCount).isEqualTo(1);
        List<RestStopEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities).hasSize(1);
        assertThat(savedEntities.get(0)).isSameAs(duplicate1);
        assertThat(savedEntities.get(0).getUnitName()).isEqualTo("업데이트됨");
    }

    @Test
    @DisplayName("관리자가 잠근(adminOverridden) 기존 행은 API 값으로 갱신하지 않는다")
    void refreshRestStops_skipsUpdatingAdminOverriddenRow() {
        runTransactionCallback();
        RestStopEntity existing = RestStopEntity.from(restStopItem("001", "관리자수정이름", "A00001"));
        existing.applyAdminEdit("관리자수정이름", "9999", "관리자노선", "1.1", "2.2");
        when(restStopRepository.findAll()).thenReturn(List.of(existing));
        RestStopItem apiItem = restStopItem("001", "API이름", "A00001");
        when(exApiClient.getLocationInfoRest(1)).thenReturn(restStopResponse("SUCCESS", "1", List.of(apiItem)));

        int savedCount = restStopSyncService.refreshRestStops();

        assertThat(savedCount).isEqualTo(1);
        List<RestStopEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities.get(0).getUnitName()).isEqualTo("관리자수정이름");
        assertThat(savedEntities.get(0).getRouteNo()).isEqualTo("9999");
    }

    @Test
    @DisplayName("DB가 비어 있으면 서버 시작 시 휴게소 목록을 적재한다")
    void initializeRestStopsIfEmpty_refreshesWhenTableIsEmpty() {
        when(restStopRepository.count()).thenReturn(0L);
        runTransactionCallback();
        when(restStopRepository.findAll()).thenReturn(List.of());
        RestStopItem restStop = restStopItem("001", "서울만남(부산)휴게소");
        when(exApiClient.getLocationInfoRest(1)).thenReturn(restStopResponse("SUCCESS", "1", List.of(restStop)));

        int savedCount = restStopSyncService.initializeRestStopsIfEmpty();

        assertThat(savedCount).isEqualTo(1);
        List<RestStopEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities).extracting(RestStopEntity::getUnitCode).containsExactly("001");
    }

    @Test
    @DisplayName("DB에 데이터가 있으면 서버 시작 시 초기 적재를 생략한다")
    void initializeRestStopsIfEmpty_skipsWhenTableHasData() {
        when(restStopRepository.count()).thenReturn(1L);

        int savedCount = restStopSyncService.initializeRestStopsIfEmpty();

        assertThat(savedCount).isZero();
        verify(exApiClient, never()).getLocationInfoRest(anyInt());
        verify(restStopRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("첫 페이지 API 호출이 실패하면 예외를 전파하지 않고 DB를 조회하거나 저장하지 않는다")
    void refreshRestStops_doesNotUpsertRowsWhenFirstPageFails() {
        ExApiException exception = new ExApiException(
                "https://data.ex.co.kr/openapi/locationinfo/locationinfoRest?key=<redacted>", "failed");
        when(exApiClient.getLocationInfoRest(1)).thenThrow(exception);

        int savedCount = restStopSyncService.refreshRestStops();

        assertThat(savedCount).isZero();
        verify(restStopRepository, never()).findAll();
        verify(restStopRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("중간 페이지 API 호출이 실패해도 성공한 페이지의 휴게소 목록을 저장한다")
    void refreshRestStops_upsertsSuccessfulPagesWhenMiddlePageFails() {
        runTransactionCallback();
        when(restStopRepository.findAll()).thenReturn(List.of());
        RestStopItem first = restStopItem("001", "서울만남(부산)휴게소", "A00001");
        RestStopItem third = restStopItem("003", "기흥(부산)휴게소", "A00003");
        when(exApiClient.getLocationInfoRest(1)).thenReturn(restStopResponse("SUCCESS", "3", List.of(first)));
        when(exApiClient.getLocationInfoRest(2))
                .thenThrow(new ExApiException(
                        "https://data.ex.co.kr/openapi/locationinfo/locationinfoRest?pageNo=2", "failed"));
        when(exApiClient.getLocationInfoRest(3)).thenReturn(restStopResponse("SUCCESS", "3", List.of(third)));

        int savedCount = restStopSyncService.refreshRestStops();

        assertThat(savedCount).isEqualTo(2);
        assertThat(captureSavedEntities())
                .extracting(RestStopEntity::getServiceAreaCode)
                .containsExactly("A00001", "A00003");
    }

    @Test
    @DisplayName("API 응답 성공 여부는 Client 계약을 신뢰하고 다시 검사하지 않는다")
    void refreshRestStops_doesNotCheckApiSuccessAgain() {
        runTransactionCallback();
        when(restStopRepository.findAll()).thenReturn(List.of());
        RestStopResponse response = mock(RestStopResponse.class);
        when(response.getTotalPageCount()).thenReturn(1);
        when(response.getList()).thenReturn(List.of());
        when(exApiClient.getLocationInfoRest(1)).thenReturn(response);

        restStopSyncService.refreshRestStops();

        verify(response, never()).isSuccess();
    }

    private RestStopResponse duplicateServiceAreaCodeResponse() {
        RestStopItem first = restStopItem("001", "서울만남(부산)휴게소");
        RestStopItem second = restStopItem("001", "이름이바뀐휴게소");
        return restStopResponse("SUCCESS", "1", List.of(first, second));
    }

    private void runTransactionCallback() {
        doAnswer(invocation -> {
                    Consumer<TransactionStatus> action = invocation.getArgument(0);
                    action.accept(mock(TransactionStatus.class));
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());
    }

    @SuppressWarnings("unchecked")
    private List<RestStopEntity> captureSavedEntities() {
        ArgumentCaptor<Iterable<RestStopEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(restStopRepository).saveAll(captor.capture());

        List<RestStopEntity> entities = new ArrayList<>();
        captor.getValue().forEach(entities::add);
        return entities;
    }
}
