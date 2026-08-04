package com.restroute.domain;

import com.restroute.client.response.RestStopDetailItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "rest_stop_detail",
        indexes = {
            @Index(
                    name = "idx_rest_stop_detail_rest_stop_service_area_code",
                    columnList = "rest_stop_service_area_code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routeName;
    private String serviceAreaCode;
    private String serviceAreaName;
    private String telNo;
    private String brand;
    private String routeCode;
    private String serviceAreaCode2;
    private String svarAddr;
    private String convenience;
    private String maintenanceYn;
    private String truckSaYn;
    private String restStopServiceAreaCode;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean adminOverridden;

    private RestStopDetailEntity(RestStopDetailItem item) {
        this.routeName = item.getRouteName();
        this.serviceAreaCode = item.getServiceAreaCode();
        this.serviceAreaName = item.getServiceAreaName();
        this.telNo = item.getTelNo();
        this.brand = item.getBrand();
        this.routeCode = item.getRouteCode();
        this.serviceAreaCode2 = item.getServiceAreaCode2();
        this.svarAddr = item.getSvarAddr();
        this.convenience = item.getConvenience();
        this.maintenanceYn = item.getMaintenanceYn();
        this.truckSaYn = item.getTruckSaYn();
    }

    public void updateFrom(RestStopDetailItem item) {
        this.routeName = item.getRouteName();
        this.serviceAreaName = item.getServiceAreaName();
        this.telNo = item.getTelNo();
        this.brand = item.getBrand();
        this.routeCode = item.getRouteCode();
        this.serviceAreaCode2 = item.getServiceAreaCode2();
        this.svarAddr = item.getSvarAddr();
        this.convenience = item.getConvenience();
        this.maintenanceYn = item.getMaintenanceYn();
        this.truckSaYn = item.getTruckSaYn();
    }

    public void updateRestStopServiceAreaCode(String restStopServiceAreaCode) {
        this.restStopServiceAreaCode = restStopServiceAreaCode;
    }

    public static RestStopDetailEntity from(RestStopDetailItem item) {
        return new RestStopDetailEntity(item);
    }

    public static RestStopDetailEntity createEmpty(String serviceAreaCode) {
        RestStopDetailEntity entity = new RestStopDetailEntity();
        entity.serviceAreaCode = serviceAreaCode;
        return entity;
    }

    public void applyAdminEdit(
            String telNo,
            String brand,
            String routeCode,
            String svarAddr,
            String convenience,
            String maintenanceYn,
            String truckSaYn) {
        this.telNo = telNo;
        this.brand = brand;
        this.routeCode = routeCode;
        this.svarAddr = svarAddr;
        this.convenience = convenience;
        this.maintenanceYn = maintenanceYn;
        this.truckSaYn = truckSaYn;
        this.adminOverridden = true;
    }

    public void clearAdminOverride() {
        this.adminOverridden = false;
    }

    public boolean isSyncable() {
        return !adminOverridden;
    }
}
