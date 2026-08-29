package com.restroute.reststop.service.restroom.util;

import com.restroute.reststop.service.restroom.dto.RestStopRestroomRow;
import com.restroute.reststop.service.restroom.exception.RestStopRestroomUploadException;
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
public class RestStopRestroomCsvParser {

    private static final Charset CSV_CHARSET = Charset.forName("MS949");
    private static final String HEADER_ROUTE_NAME = "노선";
    private static final String HEADER_REST_STOP_NAME = "시설명";
    private static final String HEADER_MALE_TOILET_COUNT = "남자_변기수";
    private static final String HEADER_FEMALE_TOILET_COUNT = "여자_변기수";
    private static final Set<String> REQUIRED_HEADERS =
            Set.of(HEADER_ROUTE_NAME, HEADER_REST_STOP_NAME, HEADER_MALE_TOILET_COUNT, HEADER_FEMALE_TOILET_COUNT);

    public List<RestStopRestroomRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RestStopRestroomUploadException("화장실 현황 CSV 파일을 첨부해야 합니다.");
        }

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get()
                .parse(new InputStreamReader(file.getInputStream(), CSV_CHARSET))) {
            validateHeaders(parser.getHeaderNames());
            List<RestStopRestroomRow> rows = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                rows.add(toRow(csvRecord));
            }
            if (rows.isEmpty()) {
                throw new RestStopRestroomUploadException("화장실 현황 CSV에 데이터가 없습니다.");
            }
            return rows;
        } catch (IOException e) {
            throw new RestStopRestroomUploadException("화장실 현황 CSV를 읽을 수 없습니다.", e);
        }
    }

    private void validateHeaders(List<String> actualHeaders) {
        if (!actualHeaders.containsAll(REQUIRED_HEADERS)) {
            throw new RestStopRestroomUploadException("화장실 현황 CSV 필수 헤더가 올바르지 않습니다.");
        }
    }

    private RestStopRestroomRow toRow(CSVRecord csvRecord) {
        return new RestStopRestroomRow(
                required(csvRecord, HEADER_ROUTE_NAME),
                required(csvRecord, HEADER_REST_STOP_NAME),
                required(csvRecord, HEADER_MALE_TOILET_COUNT),
                required(csvRecord, HEADER_FEMALE_TOILET_COUNT));
    }

    private String required(CSVRecord csvRecord, String header) {
        String value = csvRecord.get(header).trim();
        if (value.isEmpty()) {
            throw new RestStopRestroomUploadException(header + " 값이 비어 있습니다.");
        }
        return value;
    }
}
