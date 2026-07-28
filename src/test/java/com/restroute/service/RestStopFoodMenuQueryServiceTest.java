package com.restroute.service;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.RestBestfoodItem;
import com.restroute.controller.response.FoodMenuResponse;
import com.restroute.domain.RestFoodEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopFoodMenuQueryServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    private RestStopFoodMenuQueryService restStopFoodMenuQueryService;

    @BeforeEach
    void setUp() {
        restStopFoodMenuQueryService =
                new RestStopFoodMenuQueryService(restStopRepository, restStopRelatedInfoQueryService);
    }

    @Test
    @DisplayName("휴게소 기준 음식 메뉴 정보를 조회한다")
    void findByServiceAreaCode_returnsFoodMenu() throws Exception {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestFoodEntity recommendedFood = foodEntity("농심어묵우동", "Y", "Y", "N", "S");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(recommendedFood),
                        List.of(),
                        List.of()));

        Optional<FoodMenuResponse> result = restStopFoodMenuQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().menus())
                .extracting("foodName", "representative", "bestFood", "seasonLabel")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("농심어묵우동", true, true, "여름"));
        assertThat(result.get().sections()).extracting("key").containsExactly("recommended", "seasonal");
    }

    @Test
    @DisplayName("휴게소가 없으면 음식 메뉴 정보가 없다")
    void findByServiceAreaCode_returnsEmptyWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<FoodMenuResponse> result = restStopFoodMenuQueryService.findByServiceAreaCode("UNKNOWN");

        assertThat(result).isEmpty();
        verify(restStopRelatedInfoQueryService, never()).findByRestStop(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("휴게소는 있지만 음식 목록이 없으면 빈 음식 메뉴 응답을 반환한다")
    void findByServiceAreaCode_returnsEmptyFoodMenuWhenFoodsMissing() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of()));

        Optional<FoodMenuResponse> result = restStopFoodMenuQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().menus()).isEmpty();
        assertThat(result.get().sections()).isEmpty();
    }

    private RestFoodEntity foodEntity(
            String foodName, String recommendYn, String bestFoodYn, String premiumYn, String seasonMenu)
            throws Exception {
        String json = "{\"stdRestCd\":\"000001\",\"foodNm\":\""
                + foodName
                + "\",\"foodCost\":\"7000\",\"foodMaterial\":\"시원한 우동\",\"recommendyn\":\""
                + recommendYn
                + "\",\"bestfoodyn\":\""
                + bestFoodYn
                + "\",\"premiumyn\":\""
                + premiumYn
                + "\",\"seasonMenu\":\""
                + seasonMenu
                + "\"}";
        return RestFoodEntity.from(new ObjectMapper().readValue(json, RestBestfoodItem.class));
    }
}
