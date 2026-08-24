package com.restroute.reststopcontent.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestEventItem {

    private String stdRestCd;
    private String stdRestNm;
    private String eventSeq;
    private String eventNm;
    private String eventDetail;
    private String stime;
    private String etime;
    private String stdRestGubun;
    private String lastId;
    private String lastDtime;
    private String lsttmAltrUser;
    private String lsttmAltrDttm;
    private String svarAddr;
    private String routeCd;
    private String routeNm;
}
