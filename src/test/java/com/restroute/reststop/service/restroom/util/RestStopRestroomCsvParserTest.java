package com.restroute.reststop.service.restroom.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.reststop.service.restroom.dto.RestStopRestroomRow;
import com.restroute.reststop.service.restroom.exception.RestStopRestroomUploadException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class RestStopRestroomCsvParserTest {

    private final RestStopRestroomCsvParser parser = new RestStopRestroomCsvParser();

    @Test
    void parsesCp949RestroomCsv() {
        MockMultipartFile file = csv("노선,시설명,남자_변기수,여자_변기수\n" + "경부선,죽전(서울),37,57\n");

        assertThat(parser.parse(file))
                .extracting(
                        RestStopRestroomRow::sourceRestStopName,
                        RestStopRestroomRow::maleToiletCount,
                        RestStopRestroomRow::femaleToiletCount)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("죽전(서울)", "37", "57"));
    }

    @Test
    void rejectsMissingRequiredHeader() {
        MockMultipartFile file = csv("노선,시설명\n경부선,죽전(서울)\n");

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(RestStopRestroomUploadException.class)
                .hasMessageContaining("필수 헤더");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> parser.parse(new MockMultipartFile("file", new byte[0])))
                .isInstanceOf(RestStopRestroomUploadException.class);
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(RestStopRestroomUploadException.class);
    }

    @Test
    void rejectsHeaderOnlyFile() {
        MockMultipartFile file = csv("노선,시설명,남자_변기수,여자_변기수\n");

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(RestStopRestroomUploadException.class)
                .hasMessageContaining("데이터가 없습니다");
    }

    @Test
    void rejectsEmptyRowValue() {
        MockMultipartFile file = csv("노선,시설명,남자_변기수,여자_변기수\n" + "경부선,죽전(서울),,57\n");

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(RestStopRestroomUploadException.class)
                .hasMessageContaining("남자_변기수");
    }

    @Test
    void wrapsReadFailure() {
        assertThatThrownBy(() -> parser.parse(new FailingMultipartFile()))
                .isInstanceOf(RestStopRestroomUploadException.class)
                .hasMessageContaining("읽을 수 없습니다");
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "restroom.csv", "text/csv", content.getBytes(Charset.forName("MS949")));
    }

    private static class FailingMultipartFile extends MockMultipartFile {
        private FailingMultipartFile() {
            super("file", "restroom.csv", "text/csv", new byte[] {1});
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException("read failed");
        }
    }
}
