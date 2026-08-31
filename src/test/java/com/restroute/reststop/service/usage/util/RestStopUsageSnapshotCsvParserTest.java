package com.restroute.reststop.service.usage.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.reststop.service.usage.dto.RestStopUsageSnapshotRow;
import com.restroute.reststop.service.usage.exception.RestStopUsageSnapshotUploadException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class RestStopUsageSnapshotCsvParserTest {

    private static final String HEADER = "순번,노선명,휴게소명,소재지,부지면적(제곱미터),대형,소형,장애인,운영형태,1일 이용객수(17년),1일 차량 통행량(24년)\n";

    private final RestStopUsageSnapshotCsvParser parser = new RestStopUsageSnapshotCsvParser();

    @Test
    void parsesCp949UsageSnapshotCsv() {
        MockMultipartFile file = csv(HEADER + "1,경부선,죽전(서울),경기 용인시,10000,68,147,8,임대,10764,7567\n");

        assertThat(parser.parse(file))
                .extracting(
                        RestStopUsageSnapshotRow::sourceRestStopName,
                        RestStopUsageSnapshotRow::routeName,
                        RestStopUsageSnapshotRow::siteAreaSqm,
                        RestStopUsageSnapshotRow::operationType,
                        RestStopUsageSnapshotRow::dailyVisitorCount,
                        RestStopUsageSnapshotRow::dailyTrafficVolume)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("죽전(서울)", "경부선", "10000", "임대", "10764", "7567"));
    }

    @Test
    void rejectsMissingRequiredHeader() {
        MockMultipartFile file = csv("노선명,휴게소명\n경부선,죽전(서울)\n");

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(RestStopUsageSnapshotUploadException.class)
                .hasMessageContaining("필수 헤더");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> parser.parse(new MockMultipartFile("file", new byte[0])))
                .isInstanceOf(RestStopUsageSnapshotUploadException.class);
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(RestStopUsageSnapshotUploadException.class);
    }

    @Test
    void rejectsHeaderOnlyFile() {
        MockMultipartFile file = csv(HEADER);

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(RestStopUsageSnapshotUploadException.class)
                .hasMessageContaining("데이터가 없습니다");
    }

    @Test
    void rejectsEmptyRowValue() {
        MockMultipartFile file = csv(HEADER + "1,경부선,죽전(서울),경기 용인시,10000,68,147,8,임대,,7567\n");

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(RestStopUsageSnapshotUploadException.class)
                .hasMessageContaining("1일 이용객수(17년)");
    }

    @Test
    void wrapsReadFailure() {
        assertThatThrownBy(() -> parser.parse(new FailingMultipartFile()))
                .isInstanceOf(RestStopUsageSnapshotUploadException.class)
                .hasMessageContaining("읽을 수 없습니다");
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "usage.csv", "text/csv", content.getBytes(Charset.forName("MS949")));
    }

    private static class FailingMultipartFile extends MockMultipartFile {
        private FailingMultipartFile() {
            super("file", "usage.csv", "text/csv", new byte[] {1});
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException("read failed");
        }
    }
}
