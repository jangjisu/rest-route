package com.restroute.reststop.service.salesranking;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopProductSalesRankEntity;
import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import com.restroute.reststop.repository.RestStopProductSalesRankRepository;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopStoreSalesRankRepository;
import com.restroute.reststop.service.backfill.RestStopProductSalesRankBackfiller;
import com.restroute.reststop.service.backfill.RestStopStoreSalesRankBackfiller;
import com.restroute.reststop.service.salesranking.dto.SalesRankingProductRow;
import com.restroute.reststop.service.salesranking.dto.SalesRankingStoreRow;
import com.restroute.reststop.service.salesranking.util.SalesRankingCsvParser;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 판매순위는 매달 CSV로만 갱신되므로, 업로드 요청 하나가 저장 + 이름매칭까지 끝낸다.
 * 자동 동기화 도메인들이 쓰는 공용 RestStopServiceAreaCodeBackfillService에는 얹지 않는다
 * (업로드 후 "전체 휴게소명 매핑"을 따로 눌러야 하는 걸 잊어버리는 문제를 구조적으로 없앰).
 */
@Service
@RequiredArgsConstructor
public class SalesRankingUploadService {

    private final SalesRankingCsvParser csvParser;
    private final RestStopProductSalesRankRepository productRepository;
    private final RestStopStoreSalesRankRepository storeRepository;
    private final RestStopRepository restStopRepository;
    private final RestStopProductSalesRankBackfiller productSalesRankBackfiller;
    private final RestStopStoreSalesRankBackfiller storeSalesRankBackfiller;
    private final TransactionTemplate transactionTemplate;

    public int uploadProducts(MultipartFile productFile) {
        List<SalesRankingProductRow> products = csvParser.parseProducts(productFile);
        transactionTemplate.executeWithoutResult(status -> {
            saveProducts(products);
            productSalesRankBackfiller.backfill(restStops());
        });
        return products.size();
    }

    public int uploadStores(MultipartFile storeFile) {
        List<SalesRankingStoreRow> stores = csvParser.parseStores(storeFile);
        transactionTemplate.executeWithoutResult(status -> {
            saveStores(stores);
            storeSalesRankBackfiller.backfill(restStops());
        });
        return stores.size();
    }

    private List<RestStopEntity> restStops() {
        return restStopRepository.findAll();
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
