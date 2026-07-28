package com.restroute.controller.response;

import static com.restroute.support.RestStopTestFixtures.restEventItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.restroute.domain.RestEventEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RestStopEventResponseTest {

    @Test
    @DisplayName("이벤트 엔티티를 이름/설명/기간 문자열로 변환한다")
    void from_mapsNameDetailAndFormattedPeriod() {
        RestEventEntity event = RestEventEntity.from(restEventItem("000001", "1665"));
        ReflectionTestUtils.setField(event, "stime", "2026-01-01");
        ReflectionTestUtils.setField(event, "etime", "2026-12-31");

        RestStopEventResponse response = RestStopEventResponse.from(List.of(event));

        assertThat(response.events())
                .extracting("name", "detail", "period")
                .containsExactly(tuple("TEN+1 이벤트", "한식당 식사 10번 이용 시 1번 무료", "2026.01.01 ~ 2026.12.31"));
    }

    @Test
    @DisplayName("이벤트가 없으면 빈 목록을 반환한다")
    void from_returnsEmptyListWhenNoEvents() {
        RestStopEventResponse response = RestStopEventResponse.from(List.of());

        assertThat(response.events()).isEmpty();
    }
}
