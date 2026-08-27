package com.restroute.reststop.service.restroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.reststop.domain.RestStopRestroomEntity;
import com.restroute.reststop.repository.RestStopRestroomRepository;
import com.restroute.reststop.service.restroom.dto.RestStopRestroomRow;
import com.restroute.reststop.service.restroom.util.RestStopRestroomCsvParser;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class RestStopRestroomUploadServiceTest {

    @Mock
    private RestStopRestroomCsvParser csvParser;

    @Mock
    private RestStopRestroomRepository restroomRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private MultipartFile restroomFile;

    private RestStopRestroomUploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new RestStopRestroomUploadService(csvParser, restroomRepository, transactionTemplate);
    }

    @Test
    void uploadsAndUpsertsByRestStopName() {
        RestStopRestroomRow row = row("죽전(서울)", "40", "60");
        RestStopRestroomEntity existing = RestStopRestroomEntity.from(row("죽전(서울)", "37", "57"));
        existing.updateRestStopServiceAreaCode("A00001");
        when(csvParser.parse(restroomFile)).thenReturn(List.of(row));
        when(restroomRepository.findAll()).thenReturn(List.of(existing));
        runTransactionCallback();

        int result = uploadService.upload(restroomFile);

        assertThat(result).isEqualTo(1);
        assertThat(existing.getMaleToiletCount()).isEqualTo("40");
        assertThat(existing.getFemaleToiletCount()).isEqualTo("60");
        assertThat(existing.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(captureSaved()).containsExactly(existing);
    }

    @Test
    void createsNewEntityWhenNameNotYetExisting() {
        RestStopRestroomRow row = row("신규휴게소", "10", "20");
        when(csvParser.parse(restroomFile)).thenReturn(List.of(row));
        when(restroomRepository.findAll()).thenReturn(List.of());
        runTransactionCallback();

        int result = uploadService.upload(restroomFile);

        assertThat(result).isEqualTo(1);
        assertThat(captureSaved())
                .singleElement()
                .satisfies(saved -> assertThat(saved.getSourceRestStopName()).isEqualTo("신규휴게소"));
    }

    private RestStopRestroomRow row(String restStopName, String maleCount, String femaleCount) {
        return new RestStopRestroomRow("경부선", restStopName, maleCount, femaleCount);
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

    private List<RestStopRestroomEntity> captureSaved() {
        ArgumentCaptor<List<RestStopRestroomEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(restroomRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
