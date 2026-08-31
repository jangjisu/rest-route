package com.restroute.reststop.service.usage.util;

import com.restroute.reststop.service.usage.dto.RestStopUsageSnapshotRow;
import com.restroute.reststop.service.usage.exception.RestStopUsageSnapshotUploadException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class RestStopUsageSnapshotCsvParser {

    private static final Charset CSV_CHARSET = Charset.forName("MS949");
    private static final String HEADER_ROUTE_NAME = "노선명";
    private static final String HEADER_REST_STOP_NAME = "휴게소명";
    private static final String HEADER_SITE_AREA = "부지면적(제곱미터)";
    private static final String HEADER_OPERATION_TYPE = "운영형태";
    private static final String HEADER_DAILY_VISITOR_COUNT = "1일 이용객수(17년)";
    private static final String HEADER_DAILY_TRAFFIC_VOLUME = "1일 차량 통행량(24년)";
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            HEADER_ROUTE_NAME,
            HEADER_REST_STOP_NAME,
            HEADER_SITE_AREA,
            HEADER_OPERATION_TYPE,
            HEADER_DAILY_VISITOR_COUNT,
            HEADER_DAILY_TRAFFIC_VOLUME);

    public List<RestStopUsageSnapshotRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RestStopUsageSnapshotUploadException("이용객 및 교통량 현황 CSV 파일을 첨부해야 합니다.");
        }

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get()
                .parse(new InputStreamReader(file.getInputStream(), CSV_CHARSET))) {
            validateHeaders(parser.getHeaderNames());
            List<RestStopUsageSnapshotRow> rows = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                rows.add(toRow(csvRecord));
            }
            if (rows.isEmpty()) {
                throw new RestStopUsageSnapshotUploadException("이용객 및 교통량 현황 CSV에 데이터가 없습니다.");
            }
            return rows;
        } catch (IOException e) {
            throw new RestStopUsageSnapshotUploadException("이용객 및 교통량 현황 CSV를 읽을 수 없습니다.", e);
        }
    }

    private void validateHeaders(List<String> actualHeaders) {
        if (!actualHeaders.containsAll(REQUIRED_HEADERS)) {
            throw new RestStopUsageSnapshotUploadException("이용객 및 교통량 현황 CSV 필수 헤더가 올바르지 않습니다.");
        }
    }

    private RestStopUsageSnapshotRow toRow(CSVRecord csvRecord) {
        return new RestStopUsageSnapshotRow(
                required(csvRecord, HEADER_ROUTE_NAME),
                required(csvRecord, HEADER_REST_STOP_NAME),
                required(csvRecord, HEADER_SITE_AREA),
                required(csvRecord, HEADER_OPERATION_TYPE),
                required(csvRecord, HEADER_DAILY_VISITOR_COUNT),
                required(csvRecord, HEADER_DAILY_TRAFFIC_VOLUME));
    }

    private String required(CSVRecord csvRecord, String header) {
        String value = csvRecord.get(header).trim();
        if (value.isEmpty()) {
            throw new RestStopUsageSnapshotUploadException(header + " 값이 비어 있습니다.");
        }
        return value;
    }
}
