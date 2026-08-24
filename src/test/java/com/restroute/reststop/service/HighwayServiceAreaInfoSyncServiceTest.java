package com.restroute.reststop.service;

import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoItem;
import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.client.ExApiClient;
import com.restroute.client.exception.ExApiException;
import com.restroute.reststop.client.response.HighwayServiceAreaInfoItem;
import com.restroute.reststop.client.response.HighwayServiceAreaInfoResponse;
import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.repository.HighwayServiceAreaInfoRepository;
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
class HighwayServiceAreaInfoSyncServiceTest {

    @Mock
    private ExApiClient exApiClient;

    @Mock
    private HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private HighwayServiceAreaInfoSyncService highwayServiceAreaInfoSyncService;

    @BeforeEach
    void setUp() {
        highwayServiceAreaInfoSyncService = new HighwayServiceAreaInfoSyncService(
                exApiClient, highwayServiceAreaInfoRepository, transactionTemplate);
    }

    @Test
    @DisplayName("고속도로 휴게소 정보 API를 호출하고, 기존에 없는 자연키는 새로 삽입한다")
    void refreshHighwayServiceAreaInfos_fetchesAndInsertsNewRows() {
        runTransactionCallback();
        when(highwayServiceAreaInfoRepository.findAll()).thenReturn(List.of());
        HighwayServiceAreaInfoItem first = highwayServiceAreaInfoItem("000561", "북대전(논산)졸음쉼터");
        HighwayServiceAreaInfoItem second = highwayServiceAreaInfoItem("000616", "김제(새만금)주유소");
        when(exApiClient.getHighwayServiceAreaInfoList())
                .thenReturn(highwayServiceAreaInfoResponse("SUCCESS", List.of(first, second)));

        int savedCount = highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos();

        assertThat(savedCount).isEqualTo(2);
        List<HighwayServiceAreaInfoEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities)
                .extracting(HighwayServiceAreaInfoEntity::getServiceAreaCode)
                .containsExactly("000561", "000616");
    }

    @Test
    @DisplayName("기존 DB에 같은 serviceAreaCode가 있으면 같은 행을 업데이트한다")
    void refreshHighwayServiceAreaInfos_updatesExistingRowWithSameServiceAreaCode() {
        runTransactionCallback();
        HighwayServiceAreaInfoItem originalItem = highwayServiceAreaInfoItem("000561", "북대전(논산)졸음쉼터");
        HighwayServiceAreaInfoEntity existing = HighwayServiceAreaInfoEntity.from(originalItem);
        when(highwayServiceAreaInfoRepository.findAll()).thenReturn(List.of(existing));
        HighwayServiceAreaInfoItem updatedItem = highwayServiceAreaInfoItem("000561", "이름이바뀐졸음쉼터");
        when(exApiClient.getHighwayServiceAreaInfoList())
                .thenReturn(highwayServiceAreaInfoResponse("SUCCESS", List.of(updatedItem)));

        int savedCount = highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos();

        assertThat(savedCount).isEqualTo(1);
        List<HighwayServiceAreaInfoEntity> savedEntities = captureSavedEntities();
        assertThat(savedEntities).hasSize(1);
        assertThat(savedEntities.get(0)).isSameAs(existing);
        assertThat(savedEntities.get(0).getServiceAreaName()).isEqualTo("이름이바뀐졸음쉼터");
    }

    @Test
    @DisplayName("같은 응답 안에 serviceAreaCode가 중복되면 한 행으로 합쳐 저장한다")
    void refreshHighwayServiceAreaInfos_mergesDuplicateServiceAreaCodesWithinSameBatch() {
        runTransactionCallback();
        when(highwayServiceAreaInfoRepository.findAll()).thenReturn(List.of());
        when(exApiClient.getHighwayServiceAreaInfoList()).thenReturn(duplicateServiceAreaCodeResponse());

        int savedCount = highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos();

        assertThat(savedCount).isEqualTo(2);
        List<HighwayServiceAreaInfoEntity> saved = captureSavedEntities();
        List<HighwayServiceAreaInfoEntity> distinctRows =
                saved.stream().distinct().toList();
        assertThat(distinctRows).hasSize(1);
        assertThat(distinctRows.get(0).getServiceAreaName()).isEqualTo("이름이바뀐졸음쉼터");
    }

    @Test
    @DisplayName("DB에 이미 같은 serviceAreaCode의 행이 두 개 있어도 예외 없이 첫 번째 행을 유지한다")
    void refreshHighwayServiceAreaInfos_toleratesPreExistingDuplicateNaturalKeysInDb() {
        runTransactionCallback();
        HighwayServiceAreaInfoItem originalItem = highwayServiceAreaInfoItem("000561", "북대전(논산)졸음쉼터");
        HighwayServiceAreaInfoEntity duplicate1 = HighwayServiceAreaInfoEntity.from(originalItem);
        HighwayServiceAreaInfoEntity duplicate2 = HighwayServiceAreaInfoEntity.from(originalItem);
        when(highwayServiceAreaInfoRepository.findAll()).thenReturn(List.of(duplicate1, duplicate2));
        HighwayServiceAreaInfoItem updatedItem = highwayServiceAreaInfoItem("000561", "이름이바뀐졸음쉼터");
        when(exApiClient.getHighwayServiceAreaInfoList())
                .thenReturn(highwayServiceAreaInfoResponse("SUCCESS", List.of(updatedItem)));

        int savedCount = highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos();

        assertThat(savedCount).isEqualTo(1);
        List<HighwayServiceAreaInfoEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(duplicate1);
        assertThat(saved.get(0).getServiceAreaName()).isEqualTo("이름이바뀐졸음쉼터");
    }

    @Test
    @DisplayName("고속도로 휴게소 정보 API 호출이 실패하면 DB를 조회하거나 저장하지 않는다")
    void refreshHighwayServiceAreaInfos_doesNotUpsertRowsWhenApiFails() {
        ExApiException exception =
                new ExApiException("https://data.ex.co.kr/openapi/restinfo/hiwaySvarInfoList?key=<redacted>", "failed");
        when(exApiClient.getHighwayServiceAreaInfoList()).thenThrow(exception);

        assertThatThrownBy(() -> highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos())
                .isSameAs(exception);

        verify(highwayServiceAreaInfoRepository, never()).findAll();
        verify(highwayServiceAreaInfoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("고속도로 휴게소 정보 API 성공 여부는 Client 계약을 신뢰하고 다시 검사하지 않는다")
    void refreshHighwayServiceAreaInfos_doesNotCheckApiSuccessAgain() {
        runTransactionCallback();
        when(highwayServiceAreaInfoRepository.findAll()).thenReturn(List.of());
        HighwayServiceAreaInfoResponse response = mock(HighwayServiceAreaInfoResponse.class);
        when(response.getList()).thenReturn(List.of());
        when(exApiClient.getHighwayServiceAreaInfoList()).thenReturn(response);

        highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos();

        verify(response, never()).isSuccess();
    }

    private HighwayServiceAreaInfoResponse duplicateServiceAreaCodeResponse() {
        HighwayServiceAreaInfoItem first = highwayServiceAreaInfoItem("000561", "북대전(논산)졸음쉼터");
        HighwayServiceAreaInfoItem second = highwayServiceAreaInfoItem("000561", "이름이바뀐졸음쉼터");
        return highwayServiceAreaInfoResponse("SUCCESS", List.of(first, second));
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
    private List<HighwayServiceAreaInfoEntity> captureSavedEntities() {
        ArgumentCaptor<Iterable<HighwayServiceAreaInfoEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(highwayServiceAreaInfoRepository).saveAll(captor.capture());

        List<HighwayServiceAreaInfoEntity> entities = new ArrayList<>();
        captor.getValue().forEach(entities::add);
        return entities;
    }
}
