package com.restroute.reststop.service.usage;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopUsageSnapshotEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopUsageSnapshotRepository;
import com.restroute.reststop.service.backfill.RestStopUsageSnapshotBackfiller;
import com.restroute.reststop.service.usage.dto.RestStopUsageSnapshotRow;
import com.restroute.reststop.service.usage.util.RestStopUsageSnapshotCsvParser;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 판매순위/화장실 CSV와 달리, 이 도메인은 처음부터 "업로드 = 저장 + 이름매칭 + 태그 재계산"을
 * 한 번에 끝내도록 만들었다 — 매번 CSV를 새로 올릴 때마다 전체 휴게소명 매핑 버튼을 따로
 * 눌러야 하는 걸 잊어버리는 문제(화장실 현황에서 실제로 겪음)를 이 도메인부터는 구조적으로
 * 없애기 위함. 자동 동기화 도메인(Ex-API)과 달리 이 데이터는 CSV 업로드 시점에만 바뀌므로,
 * 공용 전체 백필(RestStopServiceAreaCodeBackfillService)에는 얹지 않는다.
 */
@Service
@RequiredArgsConstructor
public class RestStopUsageSnapshotUploadService {

    private final RestStopUsageSnapshotCsvParser csvParser;
    private final RestStopUsageSnapshotRepository usageSnapshotRepository;
    private final RestStopRepository restStopRepository;
    private final RestStopUsageSnapshotBackfiller backfiller;
    private final TransactionTemplate transactionTemplate;

    public int upload(MultipartFile file) {
        List<RestStopUsageSnapshotRow> rows = csvParser.parse(file);
        transactionTemplate.executeWithoutResult(status -> saveAndBackfill(rows));
        return rows.size();
    }

    private void saveAndBackfill(List<RestStopUsageSnapshotRow> rows) {
        save(rows);
        List<RestStopEntity> restStops = restStopRepository.findAll();
        backfiller.backfillNames(restStops);
        backfiller.recomputeTopTrafficTier();
        backfiller.recomputeSizeTier();
    }

    private void save(List<RestStopUsageSnapshotRow> rows) {
        Map<String, RestStopUsageSnapshotEntity> existingByName = usageSnapshotRepository.findAll().stream()
                .collect(Collectors.toMap(
                        RestStopUsageSnapshotEntity::getSourceRestStopName,
                        Function.identity(),
                        (first, second) -> first));
        usageSnapshotRepository.saveAll(
                rows.stream().map(row -> upsert(row, existingByName)).toList());
    }

    private RestStopUsageSnapshotEntity upsert(
            RestStopUsageSnapshotRow row, Map<String, RestStopUsageSnapshotEntity> existingByName) {
        RestStopUsageSnapshotEntity entity = existingByName.get(row.sourceRestStopName());
        if (entity != null) {
            entity.updateFrom(row);
            return entity;
        }
        return existingByName.computeIfAbsent(row.sourceRestStopName(), name -> RestStopUsageSnapshotEntity.from(row));
    }
}
