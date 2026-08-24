package com.restroute.evcharger.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "ev_charger_station_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uk_ev_charger_station_mapping_stat_id", columnNames = "stat_id"),
        indexes =
                @Index(
                        name = "idx_ev_charger_station_mapping_rest_stop_service_area_code",
                        columnList = "rest_stop_service_area_code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvChargerStationMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String statId;
    private String restStopServiceAreaCode;

    private EvChargerStationMappingEntity(String statId) {
        this.statId = statId;
    }

    public static EvChargerStationMappingEntity of(String statId) {
        return new EvChargerStationMappingEntity(statId);
    }

    public void updateMatch(String restStopServiceAreaCode) {
        this.restStopServiceAreaCode = restStopServiceAreaCode;
    }
}
