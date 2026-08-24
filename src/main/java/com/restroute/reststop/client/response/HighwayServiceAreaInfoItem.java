package com.restroute.reststop.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HighwayServiceAreaInfoItem {

    @JsonProperty("svarCd")
    private String serviceAreaCode;

    @JsonProperty("svarNm")
    private String serviceAreaName;

    @JsonProperty("routeCd")
    private String routeCode;

    @JsonProperty("routeNm")
    private String routeName;

    @JsonProperty("hdqrCd")
    private String headquartersCode;

    @JsonProperty("hdqrNm")
    private String headquartersName;

    @JsonProperty("mtnofCd")
    private String branchOfficeCode;

    @JsonProperty("mtnofNm")
    private String branchOfficeName;

    @JsonProperty("svarGsstClssCd")
    private String facilityTypeCode;

    @JsonProperty("svarGsstClssNm")
    private String facilityTypeName;

    @JsonProperty("gudClssCd")
    private String directionTypeCode;

    @JsonProperty("gudClssNm")
    private String directionTypeName;

    @JsonProperty("pstnoCd")
    private String postalCode;

    @JsonProperty("svarAddr")
    private String serviceAreaAddress;

    @JsonProperty("cocrPrkgTrcn")
    private String compactCarParkingCount;

    @JsonProperty("fscarPrkgTrcn")
    private String fullSizeCarParkingCount;

    @JsonProperty("dspnPrkgTrcn")
    private String disabledParkingCount;

    @JsonProperty("bsopAdtnlFcltCd")
    private String businessFacilityCode;

    @JsonProperty("rprsTelNo")
    private String representativeTelNo;
}
