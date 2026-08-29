package com.restroute.reststop.service.restroom;

import com.restroute.reststop.domain.RestStopRestroomEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopRestroomRepository;
import com.restroute.reststop.service.backfill.RestStopRestroomBackfiller;
import com.restroute.reststop.service.restroom.dto.RestStopRestroomRow;
import com.restroute.reststop.service.restroom.util.RestStopRestroomCsvParser;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 화장실 현황은 CSV 업로드로만 갱신되므로, 업로드 요청 하나가 저장 + 이름매칭까지 끝낸다.
 * 공용 RestStopServiceAreaCodeBackfillService에는 얹지 않는다(업로드 후 "전체 휴게소명
 * 매핑"을 따로 안 눌러서 옛날 데이터가 계속 보이던 문제를 구조적으로 없앰).
 */
@Service
@RequiredArgsConstructor
public class RestStopRestroomUploadService {

    private final RestStopRestroomCsvParser csvParser;
    private final RestStopRestroomRepository restroomRepository;
    private final RestStopRepository restStopRepository;
    private final RestStopRestroomBackfiller backfiller;
    private final TransactionTemplate transactionTemplate;

    public int upload(MultipartFile file) {
        List<RestStopRestroomRow> rows = csvParser.parse(file);
        transactionTemplate.executeWithoutResult(status -> {
            save(rows);
            backfiller.backfill(restStopRepository.findAll());
        });
        return rows.size();
    }

    private void save(List<RestStopRestroomRow> rows) {
        Map<String, RestStopRestroomEntity> existingByName = restroomRepository.findAll().stream()
                .collect(Collectors.toMap(
                        RestStopRestroomEntity::getSourceRestStopName, Function.identity(), (first, second) -> first));
        restroomRepository.saveAll(
                rows.stream().map(row -> upsert(row, existingByName)).toList());
    }

    private RestStopRestroomEntity upsert(RestStopRestroomRow row, Map<String, RestStopRestroomEntity> existingByName) {
        RestStopRestroomEntity entity = existingByName.get(row.sourceRestStopName());
        if (entity != null) {
            entity.updateFrom(row);
            return entity;
        }
        return existingByName.computeIfAbsent(row.sourceRestStopName(), name -> RestStopRestroomEntity.from(row));
    }
}
