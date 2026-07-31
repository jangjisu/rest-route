package com.restroute.service.salesranking;

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
    private static final Set<String> PRODUCT_HEADERS =
            Set.of("기준년월", "휴게소내판매순위", "휴게소코드", "휴게소명", "매장코드", "매장명", "판매상품SEQ", "판매상품명");
    private static final Set<String> STORE_HEADERS =
            Set.of("기준년월", "전체판매순위", "휴게소내판매순위", "휴게소코드", "휴게소명", "매장코드", "매장명");

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
            for (CSVRecord record : parser) {
                rows.add(converter.convert(record));
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

    private SalesRankingProductRow toProductRow(CSVRecord record) {
        SalesRankingProductRow row = new SalesRankingProductRow(
                required(record, "기준년월"),
                required(record, "휴게소내판매순위"),
                required(record, "휴게소코드"),
                required(record, "휴게소명"),
                required(record, "매장코드"),
                required(record, "매장명"),
                required(record, "판매상품SEQ"),
                required(record, "판매상품명"));
        validateYearMonth(row.baseYearMonth());
        return row;
    }

    private SalesRankingStoreRow toStoreRow(CSVRecord record) {
        SalesRankingStoreRow row = new SalesRankingStoreRow(
                required(record, "기준년월"),
                required(record, "전체판매순위"),
                required(record, "휴게소내판매순위"),
                required(record, "휴게소코드"),
                required(record, "휴게소명"),
                required(record, "매장코드"),
                required(record, "매장명"));
        validateYearMonth(row.baseYearMonth());
        return row;
    }

    private String required(CSVRecord record, String header) {
        String value = record.get(header).trim();
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
        T convert(CSVRecord record);
    }
}
