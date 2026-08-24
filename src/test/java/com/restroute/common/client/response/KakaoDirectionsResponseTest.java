package com.restroute.common.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.common.client.response.KakaoDirectionsResponse.Road;
import com.restroute.common.client.response.KakaoDirectionsResponse.Route;
import com.restroute.common.client.response.KakaoDirectionsResponse.Summary;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoDirectionsResponseTest {

    @Test
    @DisplayName("result_code가 0인 첫 경로가 있으면 실패로 보지 않는다")
    void failedToRoute_falseWhenFirstRouteSucceeded() {
        KakaoDirectionsResponse response = new KakaoDirectionsResponse(List.of(new Route(0, null, List.of())));

        assertThat(response.failedToRoute()).isFalse();
    }

    @Test
    @DisplayName("첫 경로의 result_code가 0이 아니면 실패로 본다")
    void failedToRoute_trueWhenFirstRouteResultCodeNotZero() {
        KakaoDirectionsResponse response = new KakaoDirectionsResponse(List.of(new Route(104, null, List.of())));

        assertThat(response.failedToRoute()).isTrue();
    }

    @Test
    @DisplayName("경로가 비어있거나 null이면 실패로 본다")
    void failedToRoute_trueWhenRoutesEmptyOrNull() {
        assertThat(new KakaoDirectionsResponse(List.of()).failedToRoute()).isTrue();
        assertThat(new KakaoDirectionsResponse(null).failedToRoute()).isTrue();
    }

    @Test
    @DisplayName("road_details 응답의 도로 구간 필드를 매핑한다")
    void readValue_mapsRoadDetailFields() throws Exception {
        String json = """
                {
                  "name": "테헤란로",
                  "distance": 24,
                  "duration": 9,
                  "traffic_speed": 9,
                  "traffic_state": 1,
                  "vertexes": [127.0283933497317, 37.49796555162529, 127.0285038625093, 37.49816469267257]
                }
                """;

        Road road = new ObjectMapper().readValue(json, Road.class);

        assertThat(road.name()).isEqualTo("테헤란로");
        assertThat(road.distance()).isEqualTo(24L);
        assertThat(road.duration()).isEqualTo(9L);
        assertThat(road.trafficSpeed()).isEqualTo(9);
        assertThat(road.trafficState()).isEqualTo(1);
        assertThat(road.vertexes()).hasSize(4);
    }

    @Test
    @DisplayName("summary 응답의 fare.toll을 매핑한다")
    void readValue_mapsSummaryFareToll() throws Exception {
        String json = """
                {
                  "distance": 19032,
                  "duration": 3494,
                  "fare": {
                    "taxi": 22200,
                    "toll": 4500
                  }
                }
                """;

        Summary summary = new ObjectMapper().readValue(json, Summary.class);

        assertThat(summary.distance()).isEqualTo(19032L);
        assertThat(summary.duration()).isEqualTo(3494L);
        assertThat(summary.fare().toll()).isEqualTo(4500);
    }

    @Test
    @DisplayName("vertexes만 받는 보조 생성자는 나머지 필드를 null로 둔다")
    void vertexesOnlyConstructor_leavesOtherFieldsNull() {
        Road road = new Road(List.of(127.0, 37.0));

        assertThat(road.vertexes()).containsExactly(127.0, 37.0);
        assertThat(road.name()).isNull();
        assertThat(road.distance()).isNull();
        assertThat(road.duration()).isNull();
        assertThat(road.trafficSpeed()).isNull();
        assertThat(road.trafficState()).isNull();
    }
}
