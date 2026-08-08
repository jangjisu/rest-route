package com.restroute.domain;

import com.restroute.client.response.RestStopItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "rest_stop")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopEntity {

    private static final String ADMIN_CODE_PREFIX = "ADMIN-";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String unitCode;
    private String unitName;
    private String routeNo;
    private String routeName;
    private String xValue;
    private String yValue;
    private String stdRestCd;
    private String serviceAreaCode;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean adminOverridden;

    private RestStopEntity(RestStopItem item) {
        this.unitCode = item.getUnitCode();
        this.unitName = item.getUnitName();
        this.routeNo = item.getRouteNo();
        this.routeName = item.getRouteName();
        this.xValue = item.getXValue();
        this.yValue = item.getYValue();
        this.stdRestCd = item.getStdRestCd();
        this.serviceAreaCode = item.getServiceAreaCode();
    }

    public void updateFrom(RestStopItem item) {
        this.unitCode = item.getUnitCode();
        this.unitName = item.getUnitName();
        this.routeNo = item.getRouteNo();
        this.routeName = item.getRouteName();
        this.xValue = item.getXValue();
        this.yValue = item.getYValue();
        this.stdRestCd = item.getStdRestCd();
    }

    public static RestStopEntity from(RestStopItem item) {
        return new RestStopEntity(item);
    }

    private RestStopEntity(String unitName, String routeNo, String routeName, String xValue, String yValue) {
        this.unitCode = ADMIN_CODE_PREFIX + UUID.randomUUID();
        this.unitName = unitName;
        this.routeNo = routeNo;
        this.routeName = routeName;
        this.xValue = xValue;
        this.yValue = yValue;
        this.stdRestCd = ADMIN_CODE_PREFIX + UUID.randomUUID();
        this.serviceAreaCode = ADMIN_CODE_PREFIX + UUID.randomUUID();
        this.adminOverridden = true;
    }

    public static RestStopEntity createByAdmin(
            String unitName, String routeNo, String routeName, String xValue, String yValue) {
        return new RestStopEntity(unitName, routeNo, routeName, xValue, yValue);
    }

    public void applyAdminEdit(String unitName, String routeNo, String routeName, String xValue, String yValue) {
        this.unitName = unitName;
        this.routeNo = routeNo;
        this.routeName = routeName;
        this.xValue = xValue;
        this.yValue = yValue;
        this.adminOverridden = true;
    }

    public void clearAdminOverride() {
        this.adminOverridden = false;
    }

    public boolean isSyncable() {
        return !adminOverridden;
    }
}
