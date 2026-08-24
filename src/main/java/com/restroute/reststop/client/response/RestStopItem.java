package com.restroute.reststop.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopItem {

    @JsonProperty("unitCode")
    private String unitCode;

    @JsonProperty("unitName")
    private String unitName;

    @JsonProperty("routeNo")
    private String routeNo;

    @JsonProperty("routeName")
    private String routeName;

    @JsonProperty("xValue")
    private String xValue;

    @JsonProperty("yValue")
    private String yValue;

    @JsonProperty("stdRestCd")
    private String stdRestCd;

    @JsonProperty("serviceAreaCode")
    private String serviceAreaCode;
}
