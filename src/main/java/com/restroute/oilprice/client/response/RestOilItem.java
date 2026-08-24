package com.restroute.oilprice.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestOilItem {

    @JsonProperty("stdRestCd")
    private String standardRestCode;

    @JsonProperty("stdRestNm")
    private String standardRestName;

    @JsonProperty("stime")
    private String startTime;

    @JsonProperty("etime")
    private String endTime;

    @JsonProperty("redId")
    private String originalModifierId;

    @JsonProperty("redDtime")
    private String originalModifiedDateTime;

    @JsonProperty("lsttmAltrUser")
    private String lastModifiedUser;

    @JsonProperty("lsttmAltrDttm")
    private String lastModifiedDateTime;

    @JsonProperty("svarAddr")
    private String serviceAreaAddress;

    @JsonProperty("routeCd")
    private String routeCode;

    @JsonProperty("routeNm")
    private String routeName;

    @JsonProperty("psCode")
    private String convenienceCode;

    @JsonProperty("psName")
    private String convenienceName;

    @JsonProperty("psDesc")
    private String convenienceDescription;
}
