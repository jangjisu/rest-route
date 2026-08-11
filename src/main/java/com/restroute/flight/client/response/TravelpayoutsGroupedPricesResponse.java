package com.restroute.flight.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelpayoutsGroupedPricesResponse(
        boolean success, String currency, Map<String, TravelpayoutsPriceItem> data) {

    public Map<String, TravelpayoutsPriceItem> dataOrEmpty() {
        return data == null ? Map.of() : data;
    }
}
