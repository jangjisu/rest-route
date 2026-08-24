package com.restroute.reststopcontent.service;

import static com.restroute.support.RestStopTestFixtures.restThemeItem;
import static com.restroute.support.RestStopTestFixtures.restThemeResponse;
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
import com.restroute.reststopcontent.client.response.RestThemeItem;
import com.restroute.reststopcontent.client.response.RestThemeResponse;
import com.restroute.reststopcontent.domain.RestThemeEntity;
import com.restroute.reststopcontent.repository.RestThemeRepository;
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
class RestThemeSyncServiceTest {

    @Mock
    private ExApiClient exApiClient;

    @Mock
    private RestThemeRepository restThemeRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private RestThemeSyncService restThemeSyncService;

    @BeforeEach
    void setUp() {
        restThemeSyncService = new RestThemeSyncService(exApiClient, restThemeRepository, transactionTemplate);
    }

    @Test
    @DisplayName("테이블이 비어 있으면 테마를 초기 적재한다")
    void initializeRestThemesIfEmpty_refreshesWhenTableIsEmpty() {
        runTransactionCallback();
        when(restThemeRepository.count()).thenReturn(0L);
        when(restThemeRepository.findAll()).thenReturn(List.of());
        RestThemeItem item = restThemeItem("000001", "4계절 꽃이 있는 휴게소");
        when(exApiClient.getRestThemeList()).thenReturn(restThemeResponse("SUCCESS", List.of(item)));

        int savedCount = restThemeSyncService.initializeRestThemesIfEmpty();

        assertThat(savedCount).isEqualTo(1);
        assertThat(captureSavedEntities())
                .extracting(RestThemeEntity::getItemNm)
                .containsExactly("4계절 꽃이 있는 휴게소");
    }

    @Test
    @DisplayName("테이블에 데이터가 있으면 테마 초기 적재를 생략한다")
    void initializeRestThemesIfEmpty_skipsWhenTableHasData() {
        when(restThemeRepository.count()).thenReturn(1L);

        int savedCount = restThemeSyncService.initializeRestThemesIfEmpty();

        assertThat(savedCount).isZero();
        verify(exApiClient, never()).getRestThemeList();
    }

    @Test
    @DisplayName("기존 DB에 없는 자연키(stdRestCd+itemNm)의 테마는 새로 삽입한다")
    void refreshRestThemes_insertsNewRows() {
        runTransactionCallback();
        when(restThemeRepository.findAll()).thenReturn(List.of());
        RestThemeItem first = restThemeItem("000001", "4계절 꽃이 있는 휴게소");
        RestThemeItem second = restThemeItem("000001", "포토존");
        when(exApiClient.getRestThemeList()).thenReturn(restThemeResponse("SUCCESS", List.of(first, second)));

        int savedCount = restThemeSyncService.refreshRestThemes();

        assertThat(savedCount).isEqualTo(2);
        assertThat(captureSavedEntities())
                .extracting(RestThemeEntity::getItemNm)
                .containsExactly("4계절 꽃이 있는 휴게소", "포토존");
    }

    @Test
    @DisplayName("기존 DB에 같은 자연키(stdRestCd+itemNm)가 있으면 같은 행을 업데이트한다")
    void refreshRestThemes_updatesExistingRowWithSameNaturalKey() {
        runTransactionCallback();
        RestThemeItem originalItem = restThemeItem("000001", "4계절 꽃이 있는 휴게소");
        RestThemeEntity existing = RestThemeEntity.from(originalItem);
        when(restThemeRepository.findAll()).thenReturn(List.of(existing));
        RestThemeItem updatedItem = restThemeItem("000001", "4계절 꽃이 있는 휴게소");
        when(exApiClient.getRestThemeList()).thenReturn(restThemeResponse("SUCCESS", List.of(updatedItem)));

        int savedCount = restThemeSyncService.refreshRestThemes();

        assertThat(savedCount).isEqualTo(1);
        List<RestThemeEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(existing);
    }

    @Test
    @DisplayName("같은 응답 안에 자연키가 중복되면 한 행으로 합쳐 저장한다")
    void refreshRestThemes_mergesDuplicateNaturalKeysWithinSameBatch() {
        runTransactionCallback();
        when(restThemeRepository.findAll()).thenReturn(List.of());
        RestThemeItem first = restThemeItem("000001", "4계절 꽃이 있는 휴게소");
        RestThemeItem second = restThemeItem("000001", "4계절 꽃이 있는 휴게소");
        when(exApiClient.getRestThemeList()).thenReturn(restThemeResponse("SUCCESS", List.of(first, second)));

        int savedCount = restThemeSyncService.refreshRestThemes();

        assertThat(savedCount).isEqualTo(2);
        List<RestThemeEntity> distinctRows =
                captureSavedEntities().stream().distinct().toList();
        assertThat(distinctRows).hasSize(1);
    }

    @Test
    @DisplayName("DB에 이미 같은 자연키(stdRestCd+itemNm)의 행이 두 개 있어도 예외 없이 첫 번째 행을 유지한다")
    void refreshRestThemes_toleratesPreExistingDuplicateNaturalKeysInDb() {
        runTransactionCallback();
        RestThemeItem originalItem = restThemeItem("000001", "4계절 꽃이 있는 휴게소");
        RestThemeEntity duplicate1 = RestThemeEntity.from(originalItem);
        RestThemeEntity duplicate2 = RestThemeEntity.from(originalItem);
        when(restThemeRepository.findAll()).thenReturn(List.of(duplicate1, duplicate2));
        RestThemeItem updatedItem = restThemeItem("000001", "4계절 꽃이 있는 휴게소");
        when(exApiClient.getRestThemeList()).thenReturn(restThemeResponse("SUCCESS", List.of(updatedItem)));

        int savedCount = restThemeSyncService.refreshRestThemes();

        assertThat(savedCount).isEqualTo(1);
        List<RestThemeEntity> saved = captureSavedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(duplicate1);
    }

    @Test
    @DisplayName("API list가 null이면 빈 목록으로 upsert해 아무것도 저장하지 않는다")
    void refreshRestThemes_upsertsEmptyListWhenListIsNull() {
        runTransactionCallback();
        when(restThemeRepository.findAll()).thenReturn(List.of());
        RestThemeResponse response = mock(RestThemeResponse.class);
        when(response.getList()).thenReturn(null);
        when(exApiClient.getRestThemeList()).thenReturn(response);

        int savedCount = restThemeSyncService.refreshRestThemes();

        assertThat(savedCount).isZero();
        verify(restThemeRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("테마휴게소 API 호출이 실패하면 DB를 조회하거나 저장하지 않는다")
    void refreshRestThemes_doesNotUpsertRowsWhenApiFails() {
        ExApiException exception =
                new ExApiException("https://data.ex.co.kr/openapi/restinfo/restThemeList?key=<redacted>", "failed");
        when(exApiClient.getRestThemeList()).thenThrow(exception);

        assertThatThrownBy(() -> restThemeSyncService.refreshRestThemes()).isSameAs(exception);

        verify(restThemeRepository, never()).findAll();
        verify(restThemeRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("테마휴게소 API 성공 여부는 Client 계약을 신뢰하고 다시 검사하지 않는다")
    void refreshRestThemes_doesNotCheckApiSuccessAgain() {
        runTransactionCallback();
        when(restThemeRepository.findAll()).thenReturn(List.of());
        RestThemeResponse response = mock(RestThemeResponse.class);
        when(response.getList()).thenReturn(List.of());
        when(exApiClient.getRestThemeList()).thenReturn(response);

        restThemeSyncService.refreshRestThemes();

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
    private List<RestThemeEntity> captureSavedEntities() {
        ArgumentCaptor<Iterable<RestThemeEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(restThemeRepository).saveAll(captor.capture());

        List<RestThemeEntity> entities = new ArrayList<>();
        captor.getValue().forEach(entities::add);
        return entities;
    }
}
