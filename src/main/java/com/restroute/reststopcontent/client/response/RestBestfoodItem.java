package com.restroute.reststopcontent.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestBestfoodItem {

    private String stdRestCd;
    private String stdRestNm;
    private String restCd;
    private String routeCd;
    private String routeNm;

    @JsonProperty("svarAddr")
    private String serviceAreaAddress;

    private String seq;
    private String foodNm;
    private String foodCost;
    private String etc;
    private String foodMaterial;
    private String recommendyn;
    private String bestfoodyn;
    private String premiumyn;
    private String seasonMenu;
    private String app;
}
