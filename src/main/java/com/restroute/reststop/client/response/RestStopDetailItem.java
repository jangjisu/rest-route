package com.restroute.reststop.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopDetailItem {

    @JsonProperty("routeName")
    private String routeName;

    @JsonProperty("serviceAreaCode")
    private String serviceAreaCode;

    @JsonProperty("serviceAreaName")
    private String serviceAreaName;

    @JsonProperty("telNo")
    private String telNo;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("routeCode")
    private String routeCode;

    @JsonProperty("serviceAreaCode2")
    private String serviceAreaCode2;

    @JsonProperty("svarAddr")
    private String svarAddr;

    @JsonProperty("convenience")
    private String convenience;

    @JsonProperty("maintenanceYn")
    private String maintenanceYn;

    @JsonProperty("truckSaYn")
    private String truckSaYn;
}
