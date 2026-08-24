package com.restroute.common.client;

public enum ExApiResponseFormat {
    JSON("json");

    private final String value;

    ExApiResponseFormat(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
