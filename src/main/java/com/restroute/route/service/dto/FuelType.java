package com.restroute.route.service.dto;

public enum FuelType {
    GASOLINE("lowest-gasoline", "휘발유 최저가"),
    DIESEL("lowest-diesel", "경유 최저가"),
    LPG("lowest-lpg", "LPG 최저가");

    private final String tagKey;
    private final String tagLabel;

    FuelType(String tagKey, String tagLabel) {
        this.tagKey = tagKey;
        this.tagLabel = tagLabel;
    }

    public String tagKey() {
        return tagKey;
    }

    public String tagLabel() {
        return tagLabel;
    }
}
