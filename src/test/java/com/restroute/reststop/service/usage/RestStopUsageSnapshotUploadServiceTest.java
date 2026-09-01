package com.restroute.reststop.service.usage;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopUsageSnapshotEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopUsageSnapshotRepository;
import com.restroute.reststop.service.backfill.RestStopUsageSnapshotBackfiller;
import com.restroute.reststop.service.usage.dto.RestStopUsageSnapshotRow;
import com.restroute.reststop.service.usage.util.RestStopUsageSnapshotCsvParser;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class RestStopUsageSnapshotUploadServiceTest {

    @Mock
    private RestStopUsageSnapshotCsvParser csvParser;

    @Mock
    private RestStopUsageSnapshotRepository usageSnapshotRepository;

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopUsageSnapshotBackfiller backfiller;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private MultipartFile usageSnapshotFile;

    private RestStopUsageSnapshotUploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new RestStopUsageSnapshotUploadService(
                csvParser, usageSnapshotRepository, restStopRepository, backfiller, transactionTemplate);
    }

    private RestStopUsageSnapshotRow row(String name) {
        return new RestStopUsageSnapshotRow("경부선", name, "10000", "임대", "1000", "5000");
    }

    @Test
    @DisplayName("업로드 한 번으로 저장, 이름매칭, 통행량 태그 재계산까지 모두 끝낸다(공용 백필에 의존하지 않음)")
    void upload_savesAndBackfillsAndRecomputesTierInOneCall() {
        RestStopUsageSnapshotRow row = row("죽전(서울)");
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "죽전(서울)휴게소", "A00001"));
        when(csvParser.parse(usageSnapshotFile)).thenReturn(List.of(row));
        when(usageSnapshotRepository.findAll()).thenReturn(List.of());
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));
        runTransactionCallback();

        int result = uploadService.upload(usageSnapshotFile);

        assertThat(result).isEqualTo(1);
        assertThat(captureSaved())
                .singleElement()
                .satisfies(saved -> assertThat(saved.getSourceRestStopName()).isEqualTo("죽전(서울)"));
        verify(backfiller).backfillNames(List.of(restStop));
        verify(backfiller).recomputeTopTrafficTier();
    }

    @Test
    @DisplayName("업로드 시 규모 등급도 함께 재계산한다")
    void upload_alsoRecomputesSizeTier() {
        RestStopUsageSnapshotRow row = row("죽전(서울)");
        when(csvParser.parse(usageSnapshotFile)).thenReturn(List.of(row));
        when(usageSnapshotRepository.findAll()).thenReturn(List.of());
        when(restStopRepository.findAll()).thenReturn(List.of());
        runTransactionCallback();

        uploadService.upload(usageSnapshotFile);

        verify(backfiller).recomputeSizeTier();
    }

    @Test
    @DisplayName("백필은 저장이 끝난 뒤에 실행된다")
    void upload_runsBackfillAfterSave() {
        RestStopUsageSnapshotRow row = row("죽전(서울)");
        when(csvParser.parse(usageSnapshotFile)).thenReturn(List.of(row));
        when(usageSnapshotRepository.findAll()).thenReturn(List.of());
        when(restStopRepository.findAll()).thenReturn(List.of());
        runTransactionCallback();

        uploadService.upload(usageSnapshotFile);

        InOrder inOrder = Mockito.inOrder(usageSnapshotRepository, backfiller);
        inOrder.verify(usageSnapshotRepository).saveAll(any());
        inOrder.verify(backfiller).backfillNames(any());
        inOrder.verify(backfiller).recomputeTopTrafficTier();
    }

    @Test
    void upload_upsertsByRestStopName() {
        RestStopUsageSnapshotRow row = row("죽전(서울)");
        RestStopUsageSnapshotEntity existing = RestStopUsageSnapshotEntity.from(row("죽전(서울)"));
        when(csvParser.parse(usageSnapshotFile)).thenReturn(List.of(row));
        when(usageSnapshotRepository.findAll()).thenReturn(List.of(existing));
        when(restStopRepository.findAll()).thenReturn(List.of());
        runTransactionCallback();

        int result = uploadService.upload(usageSnapshotFile);

        assertThat(result).isEqualTo(1);
        assertThat(captureSaved()).containsExactly(existing);
    }

    private void runTransactionCallback() {
        doAnswer(invocation -> {
                    Consumer<TransactionStatus> callback = invocation.getArgument(0);
                    callback.accept(null);
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());
    }

    private List<RestStopUsageSnapshotEntity> captureSaved() {
        ArgumentCaptor<List<RestStopUsageSnapshotEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(usageSnapshotRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
