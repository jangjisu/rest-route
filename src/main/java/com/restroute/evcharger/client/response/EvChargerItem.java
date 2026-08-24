package com.restroute.evcharger.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvChargerItem {

    private static final String HIGHWAY_REST_STOP_KIND_DETAIL = "C001";

    private String statNm;
    private String statId;
    private String chgerId;
    private String chgerType;
    private String addr;
    private String addrDetail;
    private String location;
    private String useTime;
    private String lat;
    private String lng;
    private String busiId;
    private String bnm;
    private String busiNm;
    private String busiCall;
    private String stat;
    private String statUpdDt;
    private String lastTsdt;
    private String lastTedt;
    private String nowTsdt;
    private String powerType;
    private String output;
    private String method;
    private String zcode;
    private String zscode;
    private String kind;
    private String kindDetail;
    private String parkingFree;
    private String note;
    private String limitYn;
    private String limitDetail;
    private String delYn;
    private String delDetail;
    private String trafficYn;
    private String year;
    private String floorNum;
    private String floorType;
    private String maker;

    public boolean isHighwayRestStop() {
        return HIGHWAY_REST_STOP_KIND_DETAIL.equals(kindDetail);
    }

    public boolean hasChargerIdentity() {
        return StringUtils.hasText(statId) && StringUtils.hasText(chgerId);
    }
}
