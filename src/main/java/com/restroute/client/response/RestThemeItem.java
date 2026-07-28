package com.restroute.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestThemeItem {

    private String stdRestCd;
    private String stdRestNm;
    private String itemNm;
    private String detail;
    private String regId;
    private String regDtime;
    private String lsttmAltrUser;
    private String lsttmAltrDttm;
    private String svarAddr;
    private String routeCd;
    private String routeNm;
}
