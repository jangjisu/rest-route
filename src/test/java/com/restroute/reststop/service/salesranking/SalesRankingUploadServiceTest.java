package com.restroute.reststop.service.salesranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.reststop.domain.RestStopProductSalesRankEntity;
import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import com.restroute.reststop.repository.RestStopProductSalesRankRepository;
import com.restroute.reststop.repository.RestStopStoreSalesRankRepository;
import com.restroute.reststop.service.salesranking.dto.SalesRankingProductRow;
import com.restroute.reststop.service.salesranking.dto.SalesRankingStoreRow;
import com.restroute.reststop.service.salesranking.util.SalesRankingCsvParser;
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
class SalesRankingUploadServiceTest {

    @Mock
    private SalesRankingCsvParser csvParser;

    @Mock
    private RestStopProductSalesRankRepository productRepository;

    @Mock
    private RestStopStoreSalesRankRepository storeRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private MultipartFile productFile;

    @Mock
    private MultipartFile storeFile;

    private SalesRankingUploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService =
                new SalesRankingUploadService(csvParser, productRepository, storeRepository, transactionTemplate);
    }

    @Test
    void uploadsProductsAndUpsertsNaturalKey() {
        SalesRankingProductRow productRow = productRow("2026-06", "상품명");
        RestStopProductSalesRankEntity existingProduct = RestStopProductSalesRankEntity.from(productRow);
        existingProduct.updateRestStopServiceAreaCode("A00001");
        RestStopProductSalesRankEntity duplicateProduct = RestStopProductSalesRankEntity.from(productRow);
        when(csvParser.parseProducts(productFile)).thenReturn(List.of(productRow));
        when(productRepository.findAll()).thenReturn(List.of(existingProduct, duplicateProduct));
        runTransactionCallback();

        int result = uploadService.uploadProducts(productFile);

        assertThat(result).isEqualTo(1);
        assertThat(existingProduct.getProductName()).isEqualTo("상품명");
        assertThat(existingProduct.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(captureSavedProducts()).containsExactly(existingProduct);
    }

    @Test
    void uploadsStoresAndUpsertsNaturalKey() {
        SalesRankingStoreRow storeRow = storeRow("2026-06");
        RestStopStoreSalesRankEntity existingStore = RestStopStoreSalesRankEntity.from(storeRow);
        RestStopStoreSalesRankEntity duplicateStore = RestStopStoreSalesRankEntity.from(storeRow);
        when(csvParser.parseStores(storeFile)).thenReturn(List.of(storeRow));
        when(storeRepository.findAll()).thenReturn(List.of(existingStore, duplicateStore));
        runTransactionCallback();

        int result = uploadService.uploadStores(storeFile);

        assertThat(result).isEqualTo(1);
        assertThat(captureSavedStores()).containsExactly(existingStore);
    }

    @Test
    void uploadsProducts_createsNewEntityWhenKeyNotYetExisting() {
        SalesRankingProductRow productRow = productRow("2026-06", "신상품");
        when(csvParser.parseProducts(productFile)).thenReturn(List.of(productRow));
        when(productRepository.findAll()).thenReturn(List.of());
        runTransactionCallback();

        int result = uploadService.uploadProducts(productFile);

        assertThat(result).isEqualTo(1);
        assertThat(captureSavedProducts())
                .singleElement()
                .satisfies(saved -> assertThat(saved.getProductName()).isEqualTo("신상품"));
    }

    @Test
    void uploadsStores_createsNewEntityWhenKeyNotYetExisting() {
        SalesRankingStoreRow storeRow = storeRow("2026-06");
        when(csvParser.parseStores(storeFile)).thenReturn(List.of(storeRow));
        when(storeRepository.findAll()).thenReturn(List.of());
        runTransactionCallback();

        int result = uploadService.uploadStores(storeFile);

        assertThat(result).isEqualTo(1);
        assertThat(captureSavedStores()).hasSize(1);
    }

    private SalesRankingProductRow productRow(String month, String productName) {
        return new SalesRankingProductRow(month, "1", "S000001", "휴게소", "M001", "매장", "P001", productName);
    }

    private SalesRankingStoreRow storeRow(String month) {
        return new SalesRankingStoreRow(month, "1", "1", "S000001", "휴게소", "M001", "매장");
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

    private List<RestStopProductSalesRankEntity> captureSavedProducts() {
        ArgumentCaptor<List<RestStopProductSalesRankEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private List<RestStopStoreSalesRankEntity> captureSavedStores() {
        ArgumentCaptor<List<RestStopStoreSalesRankEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(storeRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
