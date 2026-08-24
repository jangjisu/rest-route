package com.restroute.oilprice.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestOilPriceItem {

    private String routeCode;
    private String serviceAreaCode;
    private String routeName;
    private String direction;
    private String oilCompany;
    private String lpgYn;
    private String serviceAreaName;
    private String telNo;
    private String gasolinePrice;

    @JsonProperty("diselPrice")
    private String dieselPrice;

    private String lpgPrice;
    private String numOfRows;
    private String pageNo;
    private String serviceAreaCode2;

    @JsonProperty("svarAddr")
    private String serviceAreaAddress;
}
