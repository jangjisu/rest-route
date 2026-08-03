package com.restroute.domain;

import com.restroute.client.response.RestBestfoodItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "rest_food",
        indexes = {
            @Index(name = "idx_rest_food_std_rest_cd", columnList = "std_rest_cd"),
            @Index(name = "idx_rest_food_rest_stop_service_area_code", columnList = "rest_stop_service_area_code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestFoodEntity {

    private static final String ADMIN_SEQ_PREFIX = "ADMIN-";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stdRestCd;
    private String stdRestName;
    private String restCode;
    private String routeCode;
    private String routeName;
    private String serviceAreaAddress;
    private String seq;
    private String foodName;
    private String foodCost;

    @Lob
    private String description;

    @Lob
    private String foodMaterial;

    private String recommendYn;
    private String bestFoodYn;
    private String premiumYn;
    private String seasonMenu;
    private String appExposeYn;
    private String restStopServiceAreaCode;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean adminOverridden;

    private RestFoodEntity(RestBestfoodItem item) {
        this.stdRestCd = item.getStdRestCd();
        this.stdRestName = item.getStdRestNm();
        this.restCode = item.getRestCd();
        this.routeCode = item.getRouteCd();
        this.routeName = item.getRouteNm();
        this.serviceAreaAddress = item.getServiceAreaAddress();
        this.seq = item.getSeq();
        this.foodName = item.getFoodNm();
        this.foodCost = item.getFoodCost();
        this.description = item.getEtc();
        this.foodMaterial = item.getFoodMaterial();
        this.recommendYn = item.getRecommendyn();
        this.bestFoodYn = item.getBestfoodyn();
        this.premiumYn = item.getPremiumyn();
        this.seasonMenu = item.getSeasonMenu();
        this.appExposeYn = item.getApp();
    }

    public void updateFrom(RestBestfoodItem item) {
        this.stdRestName = item.getStdRestNm();
        this.restCode = item.getRestCd();
        this.routeCode = item.getRouteCd();
        this.routeName = item.getRouteNm();
        this.serviceAreaAddress = item.getServiceAreaAddress();
        this.foodName = item.getFoodNm();
        this.foodCost = item.getFoodCost();
        this.description = item.getEtc();
        this.foodMaterial = item.getFoodMaterial();
        this.recommendYn = item.getRecommendyn();
        this.bestFoodYn = item.getBestfoodyn();
        this.premiumYn = item.getPremiumyn();
        this.seasonMenu = item.getSeasonMenu();
        this.appExposeYn = item.getApp();
    }

    public void updateRestStopServiceAreaCode(String restStopServiceAreaCode) {
        this.restStopServiceAreaCode = restStopServiceAreaCode;
    }

    public void applyAdminEdit(String foodName, String foodCost, String description) {
        this.foodName = foodName;
        this.foodCost = foodCost;
        this.description = description;
        this.adminOverridden = true;
    }

    public void clearAdminOverride() {
        this.adminOverridden = false;
    }

    public boolean isAdminCreated() {
        return seq != null && seq.startsWith(ADMIN_SEQ_PREFIX);
    }

    public boolean isSyncedFood() {
        return !isAdminCreated();
    }

    public boolean isSyncable() {
        return !adminOverridden;
    }

    public static RestFoodEntity from(RestBestfoodItem item) {
        return new RestFoodEntity(item);
    }

    public static RestFoodEntity createByAdmin(
            String restStopServiceAreaCode, String stdRestCd, String foodName, String foodCost, String description) {
        RestFoodEntity entity = new RestFoodEntity();
        entity.restStopServiceAreaCode = restStopServiceAreaCode;
        entity.stdRestCd = stdRestCd;
        entity.seq = ADMIN_SEQ_PREFIX + UUID.randomUUID();
        entity.foodName = foodName;
        entity.foodCost = foodCost;
        entity.description = description;
        entity.adminOverridden = true;
        return entity;
    }
}
