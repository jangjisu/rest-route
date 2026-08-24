package com.restroute.reststop.domain;

import com.restroute.reststop.client.response.HighwayServiceAreaInfoItem;
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
        name = "highway_service_area_info",
        indexes = {
            @Index(
                    name = "idx_highway_service_area_info_rest_stop_service_area_code",
                    columnList = "rest_stop_service_area_code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HighwayServiceAreaInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceAreaCode;
    private String serviceAreaName;
    private String routeCode;
    private String routeName;
    private String headquartersCode;
    private String headquartersName;
    private String branchOfficeCode;
    private String branchOfficeName;
    private String facilityTypeCode;
    private String facilityTypeName;
    private String directionTypeCode;
    private String directionTypeName;
    private String postalCode;
    private String serviceAreaAddress;
    private String compactCarParkingCount;
    private String fullSizeCarParkingCount;
    private String disabledParkingCount;
    private String businessFacilityCode;
    private String representativeTelNo;
    private String restStopServiceAreaCode;

    private HighwayServiceAreaInfoEntity(HighwayServiceAreaInfoItem item) {
        this.serviceAreaCode = item.getServiceAreaCode();
        this.serviceAreaName = item.getServiceAreaName();
        this.routeCode = item.getRouteCode();
        this.routeName = item.getRouteName();
        this.headquartersCode = item.getHeadquartersCode();
        this.headquartersName = item.getHeadquartersName();
        this.branchOfficeCode = item.getBranchOfficeCode();
        this.branchOfficeName = item.getBranchOfficeName();
        this.facilityTypeCode = item.getFacilityTypeCode();
        this.facilityTypeName = item.getFacilityTypeName();
        this.directionTypeCode = item.getDirectionTypeCode();
        this.directionTypeName = item.getDirectionTypeName();
        this.postalCode = item.getPostalCode();
        this.serviceAreaAddress = item.getServiceAreaAddress();
        this.compactCarParkingCount = item.getCompactCarParkingCount();
        this.fullSizeCarParkingCount = item.getFullSizeCarParkingCount();
        this.disabledParkingCount = item.getDisabledParkingCount();
        this.businessFacilityCode = item.getBusinessFacilityCode();
        this.representativeTelNo = item.getRepresentativeTelNo();
    }

    public void updateFrom(HighwayServiceAreaInfoItem item) {
        this.serviceAreaName = item.getServiceAreaName();
        this.routeCode = item.getRouteCode();
        this.routeName = item.getRouteName();
        this.headquartersCode = item.getHeadquartersCode();
        this.headquartersName = item.getHeadquartersName();
        this.branchOfficeCode = item.getBranchOfficeCode();
        this.branchOfficeName = item.getBranchOfficeName();
        this.facilityTypeCode = item.getFacilityTypeCode();
        this.facilityTypeName = item.getFacilityTypeName();
        this.directionTypeCode = item.getDirectionTypeCode();
        this.directionTypeName = item.getDirectionTypeName();
        this.postalCode = item.getPostalCode();
        this.serviceAreaAddress = item.getServiceAreaAddress();
        this.compactCarParkingCount = item.getCompactCarParkingCount();
        this.fullSizeCarParkingCount = item.getFullSizeCarParkingCount();
        this.disabledParkingCount = item.getDisabledParkingCount();
        this.businessFacilityCode = item.getBusinessFacilityCode();
        this.representativeTelNo = item.getRepresentativeTelNo();
    }

    public void updateRestStopServiceAreaCode(String restStopServiceAreaCode) {
        this.restStopServiceAreaCode = restStopServiceAreaCode;
    }

    public static HighwayServiceAreaInfoEntity from(HighwayServiceAreaInfoItem item) {
        return new HighwayServiceAreaInfoEntity(item);
    }
}
