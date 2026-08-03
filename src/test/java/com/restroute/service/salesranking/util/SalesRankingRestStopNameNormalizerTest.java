package com.restroute.service.salesranking.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SalesRankingRestStopNameNormalizerTest {

    @Test
    void removesSpacesAndPunctuationCaseInsensitively() {
        assertThat(SalesRankingRestStopNameNormalizer.normalize("서울만남(부산) 휴게소")).isEqualTo("서울만남부산휴게소");
        assertThat(SalesRankingRestStopNameNormalizer.normalize(null)).isEmpty();
    }
}
