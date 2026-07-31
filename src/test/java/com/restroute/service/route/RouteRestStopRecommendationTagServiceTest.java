package com.restroute.service.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.controller.response.RouteRestStopResponse.ComparisonSummary;
import com.restroute.controller.response.RouteRestStopResponse.RecommendationTag;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteRestStopRecommendationTagServiceTest {

    @Test
    @DisplayName("비교 요약 목록에서 최저가, 최대 주차, 먹거리, 시설 태그를 만든다")
    void create_returnsRecommendationTags() {
        RouteRestStopRecommendationTagService service = new RouteRestStopRecommendationTagService();
        RouteRestStopComparison first = comparison(summary("1,700원", "1,500원", "1,200원", 16, 1, 1));
        RouteRestStopComparison second = comparison(summary("1,650원", "1,550원", "1,100원", 63, 2, 3));

        RouteRestStopRecommendationStandards standards = service.standards(List.of(first, second));

        assertThat(service.create(first, standards))
                .extracting(RecommendationTag::label)
                .containsExactly("경유 최저가", "먹거리 있음");
        assertThat(service.create(second, standards))
                .extracting(RecommendationTag::label)
                .containsExactly("휘발유 최저가", "LPG 최저가", "주차장 큼", "먹거리 있음", "시설 많음");
    }

    private RouteRestStopComparison comparison(ComparisonSummary summary) {
        return RouteRestStopComparison.of(new RouteRestStopCandidate(null, "", false, 0, null), summary);
    }

    private ComparisonSummary summary(
            String gasolinePrice,
            String dieselPrice,
            String lpgPrice,
            Integer totalParkingCount,
            int foodMenuCount,
            int facilityCount) {
        return ComparisonSummary.of(
                gasolinePrice,
                dieselPrice,
                lpgPrice,
                null,
                null,
                null,
                totalParkingCount,
                foodMenuCount,
                facilityCount);
    }
}
