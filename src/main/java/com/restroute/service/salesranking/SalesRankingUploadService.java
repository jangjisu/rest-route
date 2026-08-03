package com.restroute.service.salesranking;

import com.restroute.domain.RestStopProductSalesRankEntity;
import com.restroute.domain.RestStopStoreSalesRankEntity;
import com.restroute.repository.RestStopProductSalesRankRepository;
import com.restroute.repository.RestStopStoreSalesRankRepository;
import com.restroute.service.salesranking.dto.SalesRankingProductRow;
import com.restroute.service.salesranking.dto.SalesRankingStoreRow;
import com.restroute.service.salesranking.util.SalesRankingCsvParser;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SalesRankingUploadService {

    private final SalesRankingCsvParser csvParser;
    private final RestStopProductSalesRankRepository productRepository;
    private final RestStopStoreSalesRankRepository storeRepository;
    private final TransactionTemplate transactionTemplate;

    public int uploadProducts(MultipartFile productFile) {
        List<SalesRankingProductRow> products = csvParser.parseProducts(productFile);
        transactionTemplate.executeWithoutResult(status -> saveProducts(products));
        return products.size();
    }

    public int uploadStores(MultipartFile storeFile) {
        List<SalesRankingStoreRow> stores = csvParser.parseStores(storeFile);
        transactionTemplate.executeWithoutResult(status -> saveStores(stores));
        return stores.size();
    }

    private void saveProducts(List<SalesRankingProductRow> rows) {
        Map<String, RestStopProductSalesRankEntity> existingByKey = productRepository.findAll().stream()
                .collect(Collectors.toMap(this::productKey, Function.identity(), (first, second) -> first));
        productRepository.saveAll(
                rows.stream().map(row -> upsertProduct(row, existingByKey)).toList());
    }

    private RestStopProductSalesRankEntity upsertProduct(
            SalesRankingProductRow row, Map<String, RestStopProductSalesRankEntity> existingByKey) {
        String key = productKey(row);
        RestStopProductSalesRankEntity entity = existingByKey.get(key);
        if (entity != null) {
            entity.updateFrom(row);
            return entity;
        }
        return existingByKey.computeIfAbsent(key, k -> RestStopProductSalesRankEntity.from(row));
    }

    private void saveStores(List<SalesRankingStoreRow> rows) {
        Map<String, RestStopStoreSalesRankEntity> existingByKey = storeRepository.findAll().stream()
                .collect(Collectors.toMap(this::storeKey, Function.identity(), (first, second) -> first));
        storeRepository.saveAll(
                rows.stream().map(row -> upsertStore(row, existingByKey)).toList());
    }

    private RestStopStoreSalesRankEntity upsertStore(
            SalesRankingStoreRow row, Map<String, RestStopStoreSalesRankEntity> existingByKey) {
        String key = storeKey(row);
        RestStopStoreSalesRankEntity entity = existingByKey.get(key);
        if (entity != null) {
            entity.updateFrom(row);
            return entity;
        }
        return existingByKey.computeIfAbsent(key, k -> RestStopStoreSalesRankEntity.from(row));
    }

    private String productKey(RestStopProductSalesRankEntity entity) {
        return String.join(
                "\n",
                entity.getBaseYearMonth(),
                entity.getSourceRestStopCode(),
                entity.getSourceStoreCode(),
                entity.getProductSequence());
    }

    private String productKey(SalesRankingProductRow row) {
        return String.join(
                "\n", row.baseYearMonth(), row.sourceRestStopCode(), row.sourceStoreCode(), row.productSequence());
    }

    private String storeKey(RestStopStoreSalesRankEntity entity) {
        return String.join(
                "\n", entity.getBaseYearMonth(), entity.getSourceRestStopCode(), entity.getSourceStoreCode());
    }

    private String storeKey(SalesRankingStoreRow row) {
        return String.join("\n", row.baseYearMonth(), row.sourceRestStopCode(), row.sourceStoreCode());
    }
}
