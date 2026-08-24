package com.restroute.oilprice.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpinetAverageOilPriceResponse(
        @JsonProperty("RESULT") Result result) {

    public boolean isSuccess() {
        return result != null && result.oil() != null;
    }

    public List<OpinetAverageOilPriceItem> oil() {
        if (result == null || result.oil() == null) {
            return List.of();
        }
        return result.oil();
    }

    public String getErrorMessage() {
        if (isSuccess()) {
            return "";
        }
        return "missing RESULT.OIL";
    }

    public record Result(@JsonProperty("OIL") List<OpinetAverageOilPriceItem> oil) {}
}
