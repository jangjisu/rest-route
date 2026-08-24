package com.restroute.reststopcontent.service;

import static com.restroute.support.RestStopTestFixtures.restThemeItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.reststopcontent.domain.RestThemeEntity;
import com.restroute.reststopcontent.repository.RestThemeRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestThemeServiceAreaCodeBackfillerTest {

    @Mock
    private RestThemeRepository restThemeRepository;

    private RestThemeServiceAreaCodeBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestThemeServiceAreaCodeBackfiller(restThemeRepository);
    }

    @Test
    @DisplayName("표준휴게소코드로 조회 맵에 있는 테마만 채우고 개수를 센다")
    void backfill_mapsMatchingRowsAndCountsThem() {
        RestThemeEntity matched = RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소"));
        RestThemeEntity unmatched = RestThemeEntity.from(restThemeItem("999999", "미매칭테마"));
        when(restThemeRepository.findAll()).thenReturn(List.of(matched, unmatched));

        int mappedCount = backfiller.backfill(Map.of("000001", "A00001"));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(matched.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(unmatched.getRestStopServiceAreaCode()).isNull();
    }
}
