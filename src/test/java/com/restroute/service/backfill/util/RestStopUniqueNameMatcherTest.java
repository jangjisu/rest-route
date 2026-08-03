package com.restroute.service.backfill.util;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.domain.RestStopEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopUniqueNameMatcherTest {

    @Test
    @DisplayName("이름이 유일하게 일치하면 그 서비스지역코드를 반환한다")
    void findUniqueServiceAreaCode_returnsCodeForUniqueMatch() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));

        String result = RestStopUniqueNameMatcher.findUniqueServiceAreaCode(List.of(restStop), "서울만남(부산)휴게소");

        assertThat(result).isEqualTo("A00001");
    }

    @Test
    @DisplayName("이름이 일치하는 후보가 여러 개면 모호하므로 null을 반환한다")
    void findUniqueServiceAreaCode_returnsNullForAmbiguousMatches() {
        RestStopEntity first = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopEntity second = RestStopEntity.from(restStopItem("002", "서울만남(부산)휴게소", "A00002"));

        String result = RestStopUniqueNameMatcher.findUniqueServiceAreaCode(List.of(first, second), "서울만남(부산)휴게소");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("일치하는 후보가 없으면 null을 반환한다")
    void findUniqueServiceAreaCode_returnsNullWhenNoMatch() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));

        String result = RestStopUniqueNameMatcher.findUniqueServiceAreaCode(List.of(restStop), "미매칭휴게소");

        assertThat(result).isNull();
    }
}
