package com.restroute.reststop.domain;

import com.restroute.reststop.service.usage.dto.RestStopUsageSnapshotRow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "rest_stop_usage_snapshot",
        indexes = {
            @Index(name = "idx_rest_stop_usage_snapshot_service_area", columnList = "rest_stop_service_area_code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopUsageSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routeName;
    private String sourceRestStopName;
    private String siteAreaSqm;
    private String operationType;
    private String dailyVisitorCount;
    private String dailyTrafficVolume;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean topTrafficTier;

    @Enumerated(EnumType.STRING)
    private SizeTier sizeTier;

    private String restStopServiceAreaCode;

    private RestStopUsageSnapshotEntity(RestStopUsageSnapshotRow row) {
        apply(row);
        this.restStopServiceAreaCode = "";
    }

    public static RestStopUsageSnapshotEntity from(RestStopUsageSnapshotRow row) {
        return new RestStopUsageSnapshotEntity(row);
    }

    public void updateFrom(RestStopUsageSnapshotRow row) {
        apply(row);
    }

    private void apply(RestStopUsageSnapshotRow row) {
        this.routeName = row.routeName();
        this.sourceRestStopName = row.sourceRestStopName();
        this.siteAreaSqm = row.siteAreaSqm();
        this.operationType = row.operationType();
        this.dailyVisitorCount = row.dailyVisitorCount();
        this.dailyTrafficVolume = row.dailyTrafficVolume();
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

    public void updateTopTrafficTier(boolean topTrafficTier) {
        this.topTrafficTier = topTrafficTier;
    }

    public void updateSizeTier(SizeTier sizeTier) {
        this.sizeTier = sizeTier;
    }
}
