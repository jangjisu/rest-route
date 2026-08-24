package com.restroute.reststopcontent.service;

import static com.restroute.support.RestStopTestFixtures.restEventItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.reststopcontent.domain.RestEventEntity;
import com.restroute.reststopcontent.repository.RestEventRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestEventServiceAreaCodeBackfillerTest {

    @Mock
    private RestEventRepository restEventRepository;

    private RestEventServiceAreaCodeBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestEventServiceAreaCodeBackfiller(restEventRepository);
    }

    @Test
    @DisplayName("표준휴게소코드로 조회 맵에 있는 이벤트만 채우고 개수를 센다")
    void backfill_mapsMatchingRowsAndCountsThem() {
        RestEventEntity matched = RestEventEntity.from(restEventItem("000001", "1665"));
        RestEventEntity unmatched = RestEventEntity.from(restEventItem("999999", "1"));
        when(restEventRepository.findAll()).thenReturn(List.of(matched, unmatched));

        int mappedCount = backfiller.backfill(Map.of("000001", "A00001"));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(matched.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(unmatched.getRestStopServiceAreaCode()).isNull();
    }
}
