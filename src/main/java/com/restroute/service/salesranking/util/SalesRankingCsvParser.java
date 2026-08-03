package com.restroute.service.salesranking.util;

import com.restroute.service.salesranking.dto.SalesRankingProductRow;
import com.restroute.service.salesranking.dto.SalesRankingStoreRow;
import com.restroute.service.salesranking.exception.SalesRankingUploadException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class SalesRankingCsvParser {

    private static final Charset CSV_CHARSET = Charset.forName("MS949");
    private static final Pattern YEAR_MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final String HEADER_BASE_YEAR_MONTH = "기준년월";
    private static final String HEADER_RANK_IN_REST_STOP = "휴게소내판매순위";
    private static final String HEADER_REST_STOP_CODE = "휴게소코드";
    private static final String HEADER_REST_STOP_NAME = "휴게소명";
    private static final String HEADER_STORE_CODE = "매장코드";
    private static final String HEADER_STORE_NAME = "매장명";
    private static final Set<String> PRODUCT_HEADERS = Set.of(
            HEADER_BASE_YEAR_MONTH,
            HEADER_RANK_IN_REST_STOP,
            HEADER_REST_STOP_CODE,
            HEADER_REST_STOP_NAME,
            HEADER_STORE_CODE,
            HEADER_STORE_NAME,
            "판매상품SEQ",
            "판매상품명");
    private static final Set<String> STORE_HEADERS = Set.of(
            HEADER_BASE_YEAR_MONTH,
            "전체판매순위",
            HEADER_RANK_IN_REST_STOP,
            HEADER_REST_STOP_CODE,
            HEADER_REST_STOP_NAME,
            HEADER_STORE_CODE,
            HEADER_STORE_NAME);

    public List<SalesRankingProductRow> parseProducts(MultipartFile file) {
        return parse(file, PRODUCT_HEADERS, this::toProductRow);
    }

    public List<SalesRankingStoreRow> parseStores(MultipartFile file) {
        return parse(file, STORE_HEADERS, this::toStoreRow);
    }

    private <T> List<T> parse(MultipartFile file, Set<String> requiredHeaders, RowConverter<T> converter) {
        if (file == null || file.isEmpty()) {
            throw new SalesRankingUploadException("판매순위 CSV 파일을 모두 첨부해야 합니다.");
        }

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get()
                .parse(new InputStreamReader(file.getInputStream(), CSV_CHARSET))) {
            validateHeaders(parser.getHeaderNames(), requiredHeaders);
            List<T> rows = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                rows.add(converter.convert(csvRecord));
            }
            if (rows.isEmpty()) {
                throw new SalesRankingUploadException("판매순위 CSV에 데이터가 없습니다.");
            }
            return rows;
        } catch (IOException e) {
            throw new SalesRankingUploadException("판매순위 CSV를 읽을 수 없습니다.");
        }
    }

    private void validateHeaders(List<String> actualHeaders, Set<String> requiredHeaders) {
        if (!actualHeaders.containsAll(requiredHeaders)) {
            throw new SalesRankingUploadException("판매순위 CSV 필수 헤더가 올바르지 않습니다.");
        }
    }

    private SalesRankingProductRow toProductRow(CSVRecord csvRecord) {
        SalesRankingProductRow row = new SalesRankingProductRow(
                required(csvRecord, HEADER_BASE_YEAR_MONTH),
                required(csvRecord, HEADER_RANK_IN_REST_STOP),
                required(csvRecord, HEADER_REST_STOP_CODE),
                required(csvRecord, HEADER_REST_STOP_NAME),
                required(csvRecord, HEADER_STORE_CODE),
                required(csvRecord, HEADER_STORE_NAME),
                required(csvRecord, "판매상품SEQ"),
                required(csvRecord, "판매상품명"));
        validateYearMonth(row.baseYearMonth());
        return row;
    }

    private SalesRankingStoreRow toStoreRow(CSVRecord csvRecord) {
        SalesRankingStoreRow row = new SalesRankingStoreRow(
                required(csvRecord, HEADER_BASE_YEAR_MONTH),
                required(csvRecord, "전체판매순위"),
                required(csvRecord, HEADER_RANK_IN_REST_STOP),
                required(csvRecord, HEADER_REST_STOP_CODE),
                required(csvRecord, HEADER_REST_STOP_NAME),
                required(csvRecord, HEADER_STORE_CODE),
                required(csvRecord, HEADER_STORE_NAME));
        validateYearMonth(row.baseYearMonth());
        return row;
    }

    private String required(CSVRecord csvRecord, String header) {
        String value = csvRecord.get(header).trim();
        if (value.isEmpty()) {
            throw new SalesRankingUploadException(header + " 값이 비어 있습니다.");
        }
        return value;
    }

    private void validateYearMonth(String value) {
        if (!YEAR_MONTH.matcher(value).matches()) {
            throw new SalesRankingUploadException("기준년월은 yyyy-MM 형식이어야 합니다.");
        }
    }

    @FunctionalInterface
    private interface RowConverter<T> {
        T convert(CSVRecord csvRecord);
    }
}
