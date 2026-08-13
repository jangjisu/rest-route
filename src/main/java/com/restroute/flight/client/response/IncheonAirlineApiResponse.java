package com.restroute.flight.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IncheonAirlineApiResponse(Response response) {

    public List<IncheonAirlineItem> itemsOrEmpty() {
        return response == null || response.body() == null || response.body().items() == null
                ? List.of()
                : response.body().items();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Body body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(List<IncheonAirlineItem> items) {}
}
