package com.restroute.reststop.service;

import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static com.restroute.support.RestStopTestFixtures.restStopDetailResponse;
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
import com.restroute.reststop.client.response.RestStopDetailItem;
import com.restroute.reststop.client.response.RestStopDetailResponse;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.repository.RestStopDetailRepository;
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
class RestStopDetailSyncServiceTest {

    @Mock
    private ExApiClient exApiClient;

    @Mock
    private RestStopDetailRepository restStopDetailRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private RestStopDetailSyncService restStopDetailSyncService;

    @BeforeEach
    void setUp() {
        restStopDetailSyncService =
                new RestStopDetailSyncService(exApiClient, restStopDetailRepository, transactionTemplate);
    }

    @Test
    @DisplayName("DB가 비어 있으면 서버 시작 시 휴게소 상세 목록을 적재한다")
    void initializeRestStopDetailsIfEmpty_refreshesWhenTableIsEmpty() {
        when(restStopDetailRepository.count()).thenReturn(0L);
        runTransactionCallback();
        when(restStopDetailRepository.findAll()).thenReturn(List.of());
        RestStopDetailItem detail = restStopDetailItem("A00078", "건천(부산)휴게소");
        when(exApiClient.getConvenienceServiceArea(1))
                .thenReturn(restStopDetailResponse("SUCCESS", "1", List.of(detail)));

        int savedCount = restStopDetailSyncService.initializeRestStopDetailsIfEmpty();

        assertThat(savedCount).isEqualTo(1);
        List<RestStopDetailEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities)
                .extracting(RestStopDetailEntity::getServiceAreaCode)
                .containsExactly("A00078");
    }

    @Test
    @DisplayName("DB에 상세 데이터가 있으면 서버 시작 시 초기 적재를 생략한다")
    void initializeRestStopDetailsIfEmpty_skipsWhenTableHasData() {
        when(restStopDetailRepository.count()).thenReturn(1L);

        int savedCount = restStopDetailSyncService.initializeRestStopDetailsIfEmpty();

        assertThat(savedCount).isZero();
        verify(exApiClient, never()).getConvenienceServiceArea(anyInt());
        verify(restStopDetailRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("총 페이지 수만큼 휴게소 상세 API를 호출하고, 기존에 없는 자연키는 새로 삽입한다")
    void refreshRestStopDetails_fetchesAllPagesAndInsertsNewRows() {
        runTransactionCallback();
        when(restStopDetailRepository.findAll()).thenReturn(List.of());
        RestStopDetailItem first = restStopDetailItem("A00078", "건천(부산)휴게소");
        RestStopDetailItem second = restStopDetailItem("A00315", "처인휴게소");
        when(exApiClient.getConvenienceServiceArea(1))
                .thenReturn(restStopDetailResponse("SUCCESS", "2", List.of(first)));
        when(exApiClient.getConvenienceServiceArea(2))
                .thenReturn(restStopDetailResponse("SUCCESS", "2", List.of(second)));

        int savedCount = restStopDetailSyncService.refreshRestStopDetails();

        assertThat(savedCount).isEqualTo(2);
        List<RestStopDetailEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities)
                .extracting(RestStopDetailEntity::getServiceAreaCode)
                .containsExactly("A00078", "A00315");
    }

    @Test
    @DisplayName("기존 DB에 같은 serviceAreaCode가 있으면 같은 행을 업데이트한다")
    void refreshRestStopDetails_updatesExistingRowWithSameServiceAreaCode() {
        runTransactionCallback();
        RestStopDetailItem originalItem = restStopDetailItem("A00078", "건천(부산)휴게소");
        RestStopDetailEntity existing = RestStopDetailEntity.from(originalItem);
        when(restStopDetailRepository.findAll()).thenReturn(List.of(existing));
        RestStopDetailItem updatedItem = restStopDetailItem("A00078", "이름이바뀐휴게소");
        when(exApiClient.getConvenienceServiceArea(1))
                .thenReturn(restStopDetailResponse("SUCCESS", "1", List.of(updatedItem)));

        int savedCount = restStopDetailSyncService.refreshRestStopDetails();

        assertThat(savedCount).isEqualTo(1);
        List<RestStopDetailEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities).hasSize(1);
        assertThat(savedEntities.get(0)).isSameAs(existing);
        assertThat(savedEntities.get(0).getServiceAreaName()).isEqualTo("이름이바뀐휴게소");
    }

    @Test
    @DisplayName("관리자가 잠근(adminOverridden) 기존 행은 API 값으로 갱신하지 않는다")
    void refreshRestStopDetails_skipsUpdatingAdminOverriddenRow() {
        runTransactionCallback();
        RestStopDetailEntity existing = RestStopDetailEntity.from(restStopDetailItem("A00078", "건천(부산)휴게소"));
        existing.applyAdminEdit("031-000-0000", "관리자브랜드", "9999", "관리자주소", "샤워실", "O", "O");
        when(restStopDetailRepository.findAll()).thenReturn(List.of(existing));
        RestStopDetailItem apiItem = restStopDetailItem("A00078", "API이름");
        when(exApiClient.getConvenienceServiceArea(1))
                .thenReturn(restStopDetailResponse("SUCCESS", "1", List.of(apiItem)));

        int savedCount = restStopDetailSyncService.refreshRestStopDetails();

        assertThat(savedCount).isEqualTo(1);
        List<RestStopDetailEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities.get(0).getBrand()).isEqualTo("관리자브랜드");
        assertThat(savedEntities.get(0).getSvarAddr()).isEqualTo("관리자주소");
    }

    @Test
    @DisplayName("같은 응답 안에 serviceAreaCode가 중복되면 한 행으로 합쳐 저장한다")
    void refreshRestStopDetails_mergesDuplicateServiceAreaCodesWithinSameBatch() {
        runTransactionCallback();
        when(restStopDetailRepository.findAll()).thenReturn(List.of());
        when(exApiClient.getConvenienceServiceArea(1)).thenReturn(duplicateServiceAreaCodeResponse());

        int savedCount = restStopDetailSyncService.refreshRestStopDetails();

        assertThat(savedCount).isEqualTo(2);
        List<RestStopDetailEntity> saved = captureSavedEntities();
        List<RestStopDetailEntity> distinctRows = saved.stream().distinct().toList();
        assertThat(distinctRows).hasSize(1);
        assertThat(distinctRows.get(0).getServiceAreaName()).isEqualTo("이름이바뀐휴게소");
    }

    @Test
    @DisplayName("DB에 이미 같은 serviceAreaCode의 행이 두 개 있어도 예외 없이 첫 번째 행을 유지한다")
    void refreshRestStopDetails_toleratesPreExistingDuplicateNaturalKeysInDb() {
        runTransactionCallback();
        RestStopDetailItem originalItem = restStopDetailItem("A00078", "건천(부산)휴게소");
        RestStopDetailEntity duplicate1 = RestStopDetailEntity.from(originalItem);
        RestStopDetailEntity duplicate2 = RestStopDetailEntity.from(originalItem);
        when(restStopDetailRepository.findAll()).thenReturn(List.of(duplicate1, duplicate2));
        RestStopDetailItem updatedItem = restStopDetailItem("A00078", "이름이바뀐휴게소");
        when(exApiClient.getConvenienceServiceArea(1))
                .thenReturn(restStopDetailResponse("SUCCESS", "1", List.of(updatedItem)));

        int savedCount = restStopDetailSyncService.refreshRestStopDetails();

        assertThat(savedCount).isEqualTo(1);
        List<RestStopDetailEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(duplicate1);
        assertThat(saved.get(0).getServiceAreaName()).isEqualTo("이름이바뀐휴게소");
    }

    @Test
    @DisplayName("첫 페이지 상세 API 호출이 실패하면 예외를 전파하지 않고 DB를 조회하거나 저장하지 않는다")
    void refreshRestStopDetails_doesNotUpsertRowsWhenFirstPageFails() {
        ExApiException exception = new ExApiException(
                "https://data.ex.co.kr/openapi/business/conveniServiceArea?key=<redacted>", "failed");
        when(exApiClient.getConvenienceServiceArea(1)).thenThrow(exception);

        int savedCount = restStopDetailSyncService.refreshRestStopDetails();

        assertThat(savedCount).isZero();
        verify(restStopDetailRepository, never()).findAll();
        verify(restStopDetailRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("중간 페이지 상세 API 호출이 실패해도 성공한 페이지의 상세 목록을 저장한다")
    void refreshRestStopDetails_upsertsSuccessfulPagesWhenMiddlePageFails() {
        runTransactionCallback();
        when(restStopDetailRepository.findAll()).thenReturn(List.of());
        RestStopDetailItem first = restStopDetailItem("A00078", "건천(부산)휴게소");
        RestStopDetailItem third = restStopDetailItem("A00315", "처인휴게소");
        when(exApiClient.getConvenienceServiceArea(1))
                .thenReturn(restStopDetailResponse("SUCCESS", "3", List.of(first)));
        when(exApiClient.getConvenienceServiceArea(2))
                .thenThrow(new ExApiException(
                        "https://data.ex.co.kr/openapi/business/conveniServiceArea?pageNo=2", "failed"));
        when(exApiClient.getConvenienceServiceArea(3))
                .thenReturn(restStopDetailResponse("SUCCESS", "3", List.of(third)));

        int savedCount = restStopDetailSyncService.refreshRestStopDetails();

        assertThat(savedCount).isEqualTo(2);
        assertThat(captureSavedEntities())
                .extracting(RestStopDetailEntity::getServiceAreaCode)
                .containsExactly("A00078", "A00315");
    }

    @Test
    @DisplayName("상세 API 응답 성공 여부는 Client 계약을 신뢰하고 다시 검사하지 않는다")
    void refreshRestStopDetails_doesNotCheckApiSuccessAgain() {
        runTransactionCallback();
        when(restStopDetailRepository.findAll()).thenReturn(List.of());
        RestStopDetailResponse response = mock(RestStopDetailResponse.class);
        when(response.getTotalPageCount()).thenReturn(1);
        when(response.getList()).thenReturn(List.of());
        when(exApiClient.getConvenienceServiceArea(1)).thenReturn(response);

        restStopDetailSyncService.refreshRestStopDetails();

        verify(response, never()).isSuccess();
    }

    private RestStopDetailResponse duplicateServiceAreaCodeResponse() {
        RestStopDetailItem first = restStopDetailItem("A00078", "건천(부산)휴게소");
        RestStopDetailItem second = restStopDetailItem("A00078", "이름이바뀐휴게소");
        return restStopDetailResponse("SUCCESS", "1", List.of(first, second));
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
    private List<RestStopDetailEntity> captureSavedEntities() {
        ArgumentCaptor<Iterable<RestStopDetailEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(restStopDetailRepository).saveAll(captor.capture());

        List<RestStopDetailEntity> entities = new ArrayList<>();
        captor.getValue().forEach(entities::add);
        return entities;
    }
}
