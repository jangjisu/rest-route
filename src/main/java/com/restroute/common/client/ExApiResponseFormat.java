package com.restroute.common.client;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ExApiResponseFormat {
    JSON("json");

    private final String value;

    public String value() {
        return value;
    }
}
