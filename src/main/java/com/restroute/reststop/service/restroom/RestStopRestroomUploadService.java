package com.restroute.reststop.service.restroom;

import com.restroute.reststop.domain.RestStopRestroomEntity;
import com.restroute.reststop.repository.RestStopRestroomRepository;
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

@Service
@RequiredArgsConstructor
public class RestStopRestroomUploadService {

    private final RestStopRestroomCsvParser csvParser;
    private final RestStopRestroomRepository restroomRepository;
    private final TransactionTemplate transactionTemplate;

    public int upload(MultipartFile file) {
        List<RestStopRestroomRow> rows = csvParser.parse(file);
        transactionTemplate.executeWithoutResult(status -> save(rows));
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
