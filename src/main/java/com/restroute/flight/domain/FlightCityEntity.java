package com.restroute.flight.domain;

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

/**
 * 항공권 검색용 국가/도시 참조 데이터 (IATA 도시 코드 기준)
 */
@Getter
@Entity
@Table(
        name = "flight_city",
        indexes = {
            @Index(name = "idx_flight_city_code", columnList = "code", unique = true),
            @Index(name = "idx_flight_city_region_group", columnList = "region_group")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightCityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nameKo;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false)
    private String countryName;

    @Column(name = "region_group")
    private String regionGroup;

    public FlightCityEntity(
            String code, String name, String nameKo, String countryCode, String countryName, String regionGroup) {
        this.code = code;
        this.name = name;
        this.nameKo = nameKo;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.regionGroup = regionGroup;
    }
}
