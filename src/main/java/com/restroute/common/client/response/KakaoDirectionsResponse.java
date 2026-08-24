package com.restroute.common.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoDirectionsResponse(List<Route> routes) {

    public boolean hasSuccessfulRoute() {
        return firstRoute() != null && Integer.valueOf(0).equals(firstRoute().resultCode());
    }

    public boolean failedToRoute() {
        return !hasSuccessfulRoute();
    }

    public Route firstRoute() {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        return routes.get(0);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(@JsonProperty("result_code") Integer resultCode, Summary summary, List<Section> sections) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(Long distance, Long duration, Fare fare) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fare(Integer toll) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(List<Road> roads) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Road(
            String name,
            Long distance,
            Long duration,
            @JsonProperty("traffic_speed") Integer trafficSpeed,
            @JsonProperty("traffic_state") Integer trafficState,
            List<Double> vertexes) {

        public Road(List<Double> vertexes) {
            this(null, null, null, null, null, vertexes);
        }
    }
}
