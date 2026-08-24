package com.restroute.reststopcontent.service;

import static com.restroute.support.RestStopTestFixtures.restEventItem;
import static com.restroute.support.RestStopTestFixtures.restEventResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.common.client.ExApiClient;
import com.restroute.common.client.exception.ExApiException;
import com.restroute.reststopcontent.client.response.RestEventItem;
import com.restroute.reststopcontent.client.response.RestEventResponse;
import com.restroute.reststopcontent.domain.RestEventEntity;
import com.restroute.reststopcontent.repository.RestEventRepository;
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
class RestEventSyncServiceTest {

    @Mock
    private ExApiClient exApiClient;

    @Mock
    private RestEventRepository restEventRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private RestEventSyncService restEventSyncService;

    @BeforeEach
    void setUp() {
        restEventSyncService = new RestEventSyncService(exApiClient, restEventRepository, transactionTemplate);
    }

    @Test
    @DisplayName("테이블이 비어 있으면 이벤트를 초기 적재한다")
    void initializeRestEventsIfEmpty_refreshesWhenTableIsEmpty() {
        runTransactionCallback();
        when(restEventRepository.count()).thenReturn(0L);
        when(restEventRepository.findAll()).thenReturn(List.of());
        RestEventItem item = restEventItem("000001", "1665");
        when(exApiClient.getRestEventList()).thenReturn(restEventResponse("SUCCESS", List.of(item)));

        int savedCount = restEventSyncService.initializeRestEventsIfEmpty();

        assertThat(savedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("테이블에 데이터가 있으면 이벤트 초기 적재를 생략한다")
    void initializeRestEventsIfEmpty_skipsWhenTableHasData() {
        when(restEventRepository.count()).thenReturn(1L);

        int savedCount = restEventSyncService.initializeRestEventsIfEmpty();

        assertThat(savedCount).isZero();
        verify(exApiClient, never()).getRestEventList();
    }

    @Test
    @DisplayName("기존 DB에 없는 자연키(stdRestCd+eventSeq)의 이벤트는 새로 삽입한다")
    void refreshRestEvents_insertsNewRows() {
        runTransactionCallback();
        when(restEventRepository.findAll()).thenReturn(List.of());
        RestEventItem first = restEventItem("000001", "1665");
        RestEventItem second = restEventItem("000001", "3021");
        when(exApiClient.getRestEventList()).thenReturn(restEventResponse("SUCCESS", List.of(first, second)));

        int savedCount = restEventSyncService.refreshRestEvents();

        assertThat(savedCount).isEqualTo(2);
        assertThat(captureSavedEntities())
                .extracting(RestEventEntity::getEventSeq)
                .containsExactly("1665", "3021");
    }

    @Test
    @DisplayName("기존 DB에 같은 자연키(stdRestCd+eventSeq)가 있으면 같은 행을 업데이트한다")
    void refreshRestEvents_updatesExistingRowWithSameNaturalKey() {
        runTransactionCallback();
        RestEventItem originalItem = restEventItem("000001", "1665");
        RestEventEntity existing = RestEventEntity.from(originalItem);
        when(restEventRepository.findAll()).thenReturn(List.of(existing));
        RestEventItem updatedItem = restEventItem("000001", "1665");
        when(exApiClient.getRestEventList()).thenReturn(restEventResponse("SUCCESS", List.of(updatedItem)));

        int savedCount = restEventSyncService.refreshRestEvents();

        assertThat(savedCount).isEqualTo(1);
        List<RestEventEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(existing);
    }

    @Test
    @DisplayName("같은 응답 안에 자연키가 중복되면 한 행으로 합쳐 저장한다")
    void refreshRestEvents_mergesDuplicateNaturalKeysWithinSameBatch() {
        runTransactionCallback();
        when(restEventRepository.findAll()).thenReturn(List.of());
        RestEventItem first = restEventItem("000001", "1665");
        RestEventItem second = restEventItem("000001", "1665");
        when(exApiClient.getRestEventList()).thenReturn(restEventResponse("SUCCESS", List.of(first, second)));

        int savedCount = restEventSyncService.refreshRestEvents();

        assertThat(savedCount).isEqualTo(2);
        List<RestEventEntity> distinctRows =
                captureSavedEntities().stream().distinct().toList();
        assertThat(distinctRows).hasSize(1);
    }

    @Test
    @DisplayName("DB에 이미 같은 자연키(stdRestCd+eventSeq)의 행이 두 개 있어도 예외 없이 첫 번째 행을 유지한다")
    void refreshRestEvents_toleratesPreExistingDuplicateNaturalKeysInDb() {
        runTransactionCallback();
        RestEventItem originalItem = restEventItem("000001", "1665");
        RestEventEntity duplicate1 = RestEventEntity.from(originalItem);
        RestEventEntity duplicate2 = RestEventEntity.from(originalItem);
        when(restEventRepository.findAll()).thenReturn(List.of(duplicate1, duplicate2));
        RestEventItem updatedItem = restEventItem("000001", "1665");
        when(exApiClient.getRestEventList()).thenReturn(restEventResponse("SUCCESS", List.of(updatedItem)));

        int savedCount = restEventSyncService.refreshRestEvents();

        assertThat(savedCount).isEqualTo(1);
        List<RestEventEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(duplicate1);
    }

    @Test
    @DisplayName("API list가 null이면 빈 목록으로 upsert해 아무것도 저장하지 않는다")
    void refreshRestEvents_upsertsEmptyListWhenListIsNull() {
        runTransactionCallback();
        when(restEventRepository.findAll()).thenReturn(List.of());
        RestEventResponse response = mock(RestEventResponse.class);
        when(response.getList()).thenReturn(null);
        when(exApiClient.getRestEventList()).thenReturn(response);

        int savedCount = restEventSyncService.refreshRestEvents();

        assertThat(savedCount).isZero();
        verify(restEventRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("휴게소 이벤트 API 호출이 실패하면 DB를 조회하거나 저장하지 않는다")
    void refreshRestEvents_doesNotUpsertRowsWhenApiFails() {
        ExApiException exception =
                new ExApiException("https://data.ex.co.kr/openapi/restinfo/restEventList?key=<redacted>", "failed");
        when(exApiClient.getRestEventList()).thenThrow(exception);

        assertThatThrownBy(() -> restEventSyncService.refreshRestEvents()).isSameAs(exception);

        verify(restEventRepository, never()).findAll();
        verify(restEventRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("휴게소 이벤트 API 성공 여부는 Client 계약을 신뢰하고 다시 검사하지 않는다")
    void refreshRestEvents_doesNotCheckApiSuccessAgain() {
        runTransactionCallback();
        when(restEventRepository.findAll()).thenReturn(List.of());
        RestEventResponse response = mock(RestEventResponse.class);
        when(response.getList()).thenReturn(List.of());
        when(exApiClient.getRestEventList()).thenReturn(response);

        restEventSyncService.refreshRestEvents();

        verify(response, never()).isSuccess();
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
    private List<RestEventEntity> captureSavedEntities() {
        ArgumentCaptor<Iterable<RestEventEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(restEventRepository).saveAll(captor.capture());

        List<RestEventEntity> entities = new ArrayList<>();
        captor.getValue().forEach(entities::add);
        return entities;
    }
}
