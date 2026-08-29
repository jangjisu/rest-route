package com.restroute.reststop.domain;

import com.restroute.reststop.service.restroom.dto.RestStopRestroomRow;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Entity
@Table(
        name = "rest_stop_restroom",
        indexes = {@Index(name = "idx_rest_stop_restroom_service_area", columnList = "rest_stop_service_area_code")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopRestroomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routeName;
    private String sourceRestStopName;
    private String maleToiletCount;
    private String femaleToiletCount;
    private String restStopServiceAreaCode;

    private RestStopRestroomEntity(RestStopRestroomRow row) {
        apply(row);
        this.restStopServiceAreaCode = "";
    }

    public static RestStopRestroomEntity from(RestStopRestroomRow row) {
        return new RestStopRestroomEntity(row);
    }

    public void updateFrom(RestStopRestroomRow row) {
        apply(row);
    }

    private void apply(RestStopRestroomRow row) {
        this.routeName = row.routeName();
        this.sourceRestStopName = row.sourceRestStopName();
        this.maleToiletCount = row.maleToiletCount();
        this.femaleToiletCount = row.femaleToiletCount();
    }

    public boolean isUnmapped() {
        return !StringUtils.hasText(restStopServiceAreaCode);
    }

    public boolean isMapped() {
        return !isUnmapped();
    }

    public void updateRestStopServiceAreaCode(String serviceAreaCode) {
        this.restStopServiceAreaCode = serviceAreaCode;
    }
}
