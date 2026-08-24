package com.restroute.oilprice.service;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static com.restroute.support.RestStopTestFixtures.restOilResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.common.client.ExApiClient;
import com.restroute.common.client.exception.ExApiException;
import com.restroute.oilprice.client.response.RestOilItem;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.repository.RestOilRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class RestOilSyncServiceTest {

    @Mock
    private ExApiClient exApiClient;

    @Mock
    private RestOilRepository restOilRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private RestOilSyncService restOilSyncService;

    @BeforeEach
    void setUp() {
        restOilSyncService = new RestOilSyncService(exApiClient, restOilRepository, transactionTemplate);
    }

    @Test
    @DisplayName("테이블이 비어 있으면 주유소 편의시설을 초기 적재한다")
    void initializeRestOilsIfEmpty_refreshesWhenTableIsEmpty() {
        runTransactionCallback();
        RestOilItem item = restOilItem("000002", "서울만남(부산)주유소");
        when(restOilRepository.count()).thenReturn(0L);
        when(restOilRepository.findAll()).thenReturn(List.of());
        when(exApiClient.getRestOilList()).thenReturn(restOilResponse("SUCCESS", List.of(item)));

        int savedCount = restOilSyncService.initializeRestOilsIfEmpty();

        assertThat(savedCount).isEqualTo(1);
        List<RestOilEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities).extracting(RestOilEntity::getStandardRestName).containsExactly("서울만남(부산)주유소");
    }

    @Test
    @DisplayName("테이블에 데이터가 있으면 초기 적재를 생략한다")
    void initializeRestOilsIfEmpty_skipsWhenTableHasData() {
        when(restOilRepository.count()).thenReturn(1L);

        int savedCount = restOilSyncService.initializeRestOilsIfEmpty();

        assertThat(savedCount).isZero();
        verify(exApiClient, never()).getRestOilList();
    }

    @Test
    @DisplayName("자연키(standardRestCode+convenienceCode)가 다른 항목은 각각 새로 삽입한다")
    void refreshRestOils_insertsRowsWithDifferentNaturalKeys() {
        runTransactionCallback();
        when(restOilRepository.findAll()).thenReturn(List.of());
        RestOilItem first = restOilItem("000002", "서울만남(부산)주유소", "07");
        RestOilItem second = restOilItem("000002", "서울만남(부산)주유소", "08");
        when(exApiClient.getRestOilList()).thenReturn(restOilResponse("SUCCESS", List.of(first, second)));

        int savedCount = restOilSyncService.refreshRestOils();

        assertThat(savedCount).isEqualTo(2);
        List<RestOilEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(RestOilEntity::getNormalizedStationName).containsExactly("서울만남(부산)", "서울만남(부산)");
    }

    @Test
    @DisplayName("기존 DB에 같은 자연키(standardRestCode+convenienceCode)가 있으면 같은 행을 업데이트한다")
    void refreshRestOils_updatesExistingRowWithSameNaturalKey() {
        runTransactionCallback();
        RestOilItem originalItem = restOilItem("000002", "서울만남(부산)주유소", "07");
        RestOilEntity existing = RestOilEntity.from(originalItem);
        when(restOilRepository.findAll()).thenReturn(List.of(existing));
        RestOilItem updatedItem = restOilItem("000002", "변경된주유소명", "07");
        when(exApiClient.getRestOilList()).thenReturn(restOilResponse("SUCCESS", List.of(updatedItem)));

        int savedCount = restOilSyncService.refreshRestOils();

        assertThat(savedCount).isEqualTo(1);
        List<RestOilEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(existing);
        assertThat(saved.get(0).getStandardRestName()).isEqualTo("변경된주유소명");
    }

    @Test
    @DisplayName("같은 응답 안에 자연키가 중복되면 한 행으로 합쳐 저장한다")
    void refreshRestOils_mergesDuplicateNaturalKeysWithinSameBatch() {
        runTransactionCallback();
        when(restOilRepository.findAll()).thenReturn(List.of());
        RestOilItem first = restOilItem("000002", "서울만남(부산)주유소", "07");
        RestOilItem second = restOilItem("000002", "변경된주유소명", "07");
        when(exApiClient.getRestOilList()).thenReturn(restOilResponse("SUCCESS", List.of(first, second)));

        int savedCount = restOilSyncService.refreshRestOils();

        assertThat(savedCount).isEqualTo(2);
        List<RestOilEntity> saved = captureSavedEntities();
        List<RestOilEntity> distinctRows = saved.stream().distinct().toList();
        assertThat(distinctRows).hasSize(1);
        assertThat(distinctRows.get(0).getStandardRestName()).isEqualTo("변경된주유소명");
    }

    @Test
    @DisplayName("DB에 이미 같은 자연키(standardRestCode+convenienceCode)의 행이 두 개 있어도 예외 없이 첫 번째 행을 유지한다")
    void refreshRestOils_toleratesPreExistingDuplicateNaturalKeysInDb() {
        runTransactionCallback();
        RestOilItem originalItem = restOilItem("000002", "서울만남(부산)주유소", "07");
        RestOilEntity duplicate1 = RestOilEntity.from(originalItem);
        RestOilEntity duplicate2 = RestOilEntity.from(originalItem);
        when(restOilRepository.findAll()).thenReturn(List.of(duplicate1, duplicate2));
        RestOilItem updatedItem = restOilItem("000002", "변경된주유소명", "07");
        when(exApiClient.getRestOilList()).thenReturn(restOilResponse("SUCCESS", List.of(updatedItem)));

        int savedCount = restOilSyncService.refreshRestOils();

        assertThat(savedCount).isEqualTo(1);
        List<RestOilEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(duplicate1);
        assertThat(saved.get(0).getStandardRestName()).isEqualTo("변경된주유소명");
    }

    @Test
    @DisplayName("주유소 편의시설 API 호출이 실패하면 DB를 조회하거나 저장하지 않는다")
    void refreshRestOils_doesNotUpsertRowsWhenApiFails() {
        ExApiException exception =
                new ExApiException("https://data.ex.co.kr/openapi/restinfo/restOilList?key=<redacted>", "failed");
        when(exApiClient.getRestOilList()).thenThrow(exception);

        assertThatThrownBy(restOilSyncService::refreshRestOils).isSameAs(exception);

        verify(restOilRepository, never()).findAll();
        verify(restOilRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("API list가 null이면 빈 목록으로 upsert해 아무것도 저장하지 않는다")
    void refreshRestOils_upsertsEmptyListWhenListIsNull() {
        runTransactionCallback();
        when(restOilRepository.findAll()).thenReturn(List.of());
        var response = restOilResponse("SUCCESS", List.of());
        ReflectionTestUtils.setField(response, "list", null);
        when(exApiClient.getRestOilList()).thenReturn(response);

        int savedCount = restOilSyncService.refreshRestOils();

        assertThat(savedCount).isZero();
        verify(restOilRepository).saveAll(List.of());
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
    private List<RestOilEntity> captureSavedEntities() {
        ArgumentCaptor<Iterable<RestOilEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(restOilRepository).saveAll(captor.capture());

        List<RestOilEntity> entities = new ArrayList<>();
        captor.getValue().forEach(entities::add);
        return entities;
    }
}
