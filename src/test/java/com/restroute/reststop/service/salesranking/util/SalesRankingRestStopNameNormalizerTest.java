package com.restroute.reststop.service.salesranking.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SalesRankingRestStopNameNormalizerTest {

    @Test
    void removesSpacesAndPunctuationCaseInsensitively() {
        assertThat(SalesRankingRestStopNameNormalizer.normalize("서울만남(부산) 휴게소")).isEqualTo("서울만남부산");
        assertThat(SalesRankingRestStopNameNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void matchesRegardlessOfRestStopSuffixPresenceOrPosition() {
        // 화장실 현황 CSV처럼 "휴게소" 표기가 아예 없거나(죽전(서울)) 다른 위치에 붙는 경우(망향휴게소(부산))가
        // 실제 rest_stop 테이블의 "죽전(서울)휴게소"/"망향(부산)휴게소"와 같은 휴게소로 매칭돼야 한다.
        assertThat(SalesRankingRestStopNameNormalizer.normalize("죽전(서울)"))
                .isEqualTo(SalesRankingRestStopNameNormalizer.normalize("죽전(서울)휴게소"));
        assertThat(SalesRankingRestStopNameNormalizer.normalize("망향휴게소(부산)"))
                .isEqualTo(SalesRankingRestStopNameNormalizer.normalize("망향(부산)휴게소"));
    }
}
