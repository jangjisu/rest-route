package com.restroute.service.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.RestBestfoodItem;
import com.restroute.domain.RestFoodEntity;
import com.restroute.repository.RestFoodRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestFoodServiceAreaCodeBackfillerTest {

    @Mock
    private RestFoodRepository restFoodRepository;

    private RestFoodServiceAreaCodeBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestFoodServiceAreaCodeBackfiller(restFoodRepository);
    }

    @Test
    @DisplayName("표준휴게소코드로 조회 맵에 있는 음식만 채우고 개수를 센다")
    void backfill_mapsMatchingRowsAndCountsThem() throws Exception {
        RestFoodEntity matched = foodEntity("000001", "한우국밥");
        RestFoodEntity unmatched = foodEntity("999999", "미매칭메뉴");
        when(restFoodRepository.findAll()).thenReturn(List.of(matched, unmatched));

        int mappedCount = backfiller.backfill(Map.of("000001", "A00001"));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(matched.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(unmatched.getRestStopServiceAreaCode()).isNull();
    }

    private RestFoodEntity foodEntity(String stdRestCd, String foodName) throws Exception {
        String json = """
                {"stdRestCd":"%s","foodNm":"%s","foodCost":"7000","recommendyn":"N"}
                """.formatted(stdRestCd, foodName);
        RestBestfoodItem item = new ObjectMapper().readValue(json, RestBestfoodItem.class);
        return RestFoodEntity.from(item);
    }
}
