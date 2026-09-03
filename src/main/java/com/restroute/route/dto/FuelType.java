package com.restroute.route.dto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FuelType {
    GASOLINE("휘발유", "gasoline", true),
    DIESEL("경유", "diesel", true),
    LPG("LPG", "lpg", true),
    EV("전기", "ev", false);

    private final String korDescription;
    private final String engDescription;
    private final boolean havePriceInfo;

    public String korDescription() {
        return korDescription;
    }

    public String engDescription() {
        return engDescription;
    }

    public boolean havePriceInfo() {
        return havePriceInfo;
    }
}
