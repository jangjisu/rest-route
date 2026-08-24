package com.restroute.reststopcontent.domain;

import com.restroute.reststopcontent.client.response.RestEventItem;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "rest_event",
        indexes = {
            @Index(name = "idx_rest_event_std_rest_cd", columnList = "std_rest_cd"),
            @Index(name = "idx_rest_event_rest_stop_service_area_code", columnList = "rest_stop_service_area_code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stdRestCd;
    private String stdRestNm;
    private String eventSeq;
    private String eventNm;

    @Lob
    private String eventDetail;

    private String stime;
    private String etime;
    private String routeCd;
    private String routeNm;
    private String svarAddr;
    private String restStopServiceAreaCode;

    private RestEventEntity(RestEventItem item) {
        this.stdRestCd = item.getStdRestCd();
        this.eventSeq = item.getEventSeq();
        apply(item);
    }

    public void updateFrom(RestEventItem item) {
        apply(item);
    }

    public void updateRestStopServiceAreaCode(String restStopServiceAreaCode) {
        this.restStopServiceAreaCode = restStopServiceAreaCode;
    }

    public static RestEventEntity from(RestEventItem item) {
        return new RestEventEntity(item);
    }

    private void apply(RestEventItem item) {
        this.stdRestNm = item.getStdRestNm();
        this.eventNm = item.getEventNm();
        this.eventDetail = item.getEventDetail();
        this.stime = item.getStime();
        this.etime = item.getEtime();
        this.routeCd = item.getRouteCd();
        this.routeNm = item.getRouteNm();
        this.svarAddr = item.getSvarAddr();
    }
}
