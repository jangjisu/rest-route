package com.restroute.common.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoLocalSearchResponse(List<Document> documents) {

    public boolean isEmpty() {
        return documents == null || documents.isEmpty();
    }

    public Document first() {
        return documents.get(0);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            String x,
            String y,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("address_name") String addressName) {

        public String label() {
            if (placeName != null && !placeName.isBlank()) {
                return placeName;
            }
            return addressName;
        }
    }
}
