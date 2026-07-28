package com.restroute.domain;

import com.restroute.client.response.RestThemeItem;
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
        name = "rest_theme",
        indexes = {
            @Index(name = "idx_rest_theme_std_rest_cd", columnList = "std_rest_cd"),
            @Index(name = "idx_rest_theme_rest_stop_service_area_code", columnList = "rest_stop_service_area_code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestThemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stdRestCd;
    private String stdRestNm;
    private String itemNm;

    @Lob
    private String detail;

    private String routeCd;
    private String routeNm;
    private String svarAddr;
    private String restStopServiceAreaCode;

    private RestThemeEntity(RestThemeItem item) {
        apply(item);
    }

    public void updateFrom(RestThemeItem item) {
        apply(item);
    }

    public void updateRestStopServiceAreaCode(String restStopServiceAreaCode) {
        this.restStopServiceAreaCode = restStopServiceAreaCode;
    }

    public static RestThemeEntity from(RestThemeItem item) {
        return new RestThemeEntity(item);
    }

    private void apply(RestThemeItem item) {
        this.stdRestCd = item.getStdRestCd();
        this.stdRestNm = item.getStdRestNm();
        this.itemNm = item.getItemNm();
        this.detail = item.getDetail();
        this.routeCd = item.getRouteCd();
        this.routeNm = item.getRouteNm();
        this.svarAddr = item.getSvarAddr();
    }
}
