package com.restroute.service.salesranking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.service.salesranking.dto.SalesRankingProductRow;
import com.restroute.service.salesranking.dto.SalesRankingStoreRow;
import com.restroute.service.salesranking.exception.SalesRankingUploadException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class SalesRankingCsvParserTest {

    private final SalesRankingCsvParser parser = new SalesRankingCsvParser();

    @Test
    void parsesCp949ProductCsv() {
        MockMultipartFile file = csv("기준년월,휴게소내판매순위,휴게소코드,휴게소명,매장코드,매장명,판매상품SEQ,판매상품명\n"
                + "2026-06,1,S000001,서울만남(부산)휴게소,0025,말죽거리한식당,1100000709,말죽거리소고기국밥\n");

        assertThat(parser.parseProducts(file))
                .extracting(SalesRankingProductRow::productName)
                .containsExactly("말죽거리소고기국밥");
    }

    @Test
    void rejectsMissingRequiredHeader() {
        MockMultipartFile file = csv("기준년월,휴게소코드,휴게소명\n2026-06,S000001,휴게소\n");

        assertThatThrownBy(() -> parser.parseProducts(file))
                .isInstanceOf(SalesRankingUploadException.class)
                .hasMessageContaining("필수 헤더");
    }

    @Test
    void rejectsInvalidBaseYearMonth() {
        MockMultipartFile file =
                csv("기준년월,휴게소내판매순위,휴게소코드,휴게소명,매장코드,매장명,판매상품SEQ,판매상품명\n" + "202606,1,S000001,휴게소,0025,매장,1,상품\n");

        assertThatThrownBy(() -> parser.parseProducts(file))
                .isInstanceOf(SalesRankingUploadException.class)
                .hasMessageContaining("yyyy-MM");
    }

    @Test
    void parsesStoreCsv() {
        MockMultipartFile file =
                csv("기준년월,전체판매순위,휴게소내판매순위,휴게소코드,휴게소명,매장코드,매장명\n" + "2026-06,1,1,S000001,휴게소,M001,매장\n");

        assertThat(parser.parseStores(file))
                .extracting(SalesRankingStoreRow::sourceStoreName)
                .containsExactly("매장");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> parser.parseProducts(new MockMultipartFile("file", new byte[0])))
                .isInstanceOf(SalesRankingUploadException.class);
        assertThatThrownBy(() -> parser.parseProducts(null)).isInstanceOf(SalesRankingUploadException.class);
    }

    @Test
    void rejectsHeaderOnlyFile() {
        MockMultipartFile file = csv("기준년월,휴게소내판매순위,휴게소코드,휴게소명,매장코드,매장명,판매상품SEQ,판매상품명\n");

        assertThatThrownBy(() -> parser.parseProducts(file))
                .isInstanceOf(SalesRankingUploadException.class)
                .hasMessageContaining("데이터가 없습니다");
    }

    @Test
    void rejectsEmptyRowValue() {
        MockMultipartFile file =
                csv("기준년월,휴게소내판매순위,휴게소코드,휴게소명,매장코드,매장명,판매상품SEQ,판매상품명\n" + "2026-06,1,S000001,휴게소,M001,매장,P001,\n");

        assertThatThrownBy(() -> parser.parseProducts(file))
                .isInstanceOf(SalesRankingUploadException.class)
                .hasMessageContaining("판매상품명");
    }

    @Test
    void wrapsReadFailure() {
        assertThatThrownBy(() -> parser.parseProducts(new FailingMultipartFile()))
                .isInstanceOf(SalesRankingUploadException.class)
                .hasMessageContaining("읽을 수 없습니다");
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "ranking.csv", "text/csv", content.getBytes(Charset.forName("MS949")));
    }

    private static class FailingMultipartFile extends MockMultipartFile {
        private FailingMultipartFile() {
            super("file", "ranking.csv", "text/csv", new byte[] {1});
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException("read failed");
        }
    }
}
