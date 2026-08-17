package com.restroute.domain;

import com.restroute.client.response.RestOilPriceItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "rest_oil_price",
        indexes = {
            @Index(name = "idx_rest_oil_price_service_area_code2", columnList = "service_area_code2"),
            @Index(name = "idx_rest_oil_price_rest_stop_service_area_code", columnList = "rest_stop_service_area_code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestOilPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routeCode;
    private String serviceAreaCode;
    private String routeName;
    private String direction;
    private String oilCompany;
    private String lpgYn;
    private String serviceAreaName;
    private String telNo;
    private String gasolinePrice;
    private String dieselPrice;
    private String lpgPrice;
    private String numOfRows;
    private String pageNo;

    @Column(name = "service_area_code2")
    private String serviceAreaCode2;

    private String serviceAreaAddress;
    private LocalDateTime lastRefreshedAt;
    private String restStopServiceAreaCode;

    private RestOilPriceEntity(RestOilPriceItem item) {
        apply(item);
    }

    private RestOilPriceEntity(RestOilPriceItem item, LocalDateTime refreshedAt) {
        apply(item);
        this.lastRefreshedAt = refreshedAt;
    }

    public static RestOilPriceEntity from(RestOilPriceItem item) {
        return new RestOilPriceEntity(item);
    }

    public static RestOilPriceEntity from(RestOilPriceItem item, LocalDateTime refreshedAt) {
        return new RestOilPriceEntity(item, refreshedAt);
    }

    public void updateFrom(RestOilPriceItem item) {
        apply(item);
    }

    public void updateFrom(RestOilPriceItem item, LocalDateTime refreshedAt) {
        apply(item);
        this.lastRefreshedAt = refreshedAt;
    }

    public void updateRestStopServiceAreaCode(String restStopServiceAreaCode) {
        this.restStopServiceAreaCode = restStopServiceAreaCode;
    }

    private void apply(RestOilPriceItem item) {
        this.routeCode = item.getRouteCode();
        this.serviceAreaCode = item.getServiceAreaCode();
        this.routeName = item.getRouteName();
        this.direction = item.getDirection();
        this.oilCompany = item.getOilCompany();
        this.lpgYn = item.getLpgYn();
        this.serviceAreaName = item.getServiceAreaName();
        this.telNo = item.getTelNo();
        this.gasolinePrice = item.getGasolinePrice();
        this.dieselPrice = item.getDieselPrice();
        this.lpgPrice = item.getLpgPrice();
        this.numOfRows = item.getNumOfRows();
        this.pageNo = item.getPageNo();
        this.serviceAreaCode2 = item.getServiceAreaCode2();
        this.serviceAreaAddress = item.getServiceAreaAddress();
    }
}
