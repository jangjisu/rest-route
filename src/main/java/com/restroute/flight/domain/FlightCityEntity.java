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
 * 항공권 검색용 도시 참조 데이터 (Travelpayouts cities.json 기준, 취항 공항이 있는 도시만).
 * 원본 데이터에 국가 전체 이름/한글 번역이 없어 code/name/countryCode만 그대로 저장한다.
 */
@Getter
@Entity
@Table(
        name = "flight_city",
        indexes = {
            @Index(name = "idx_flight_city_code", columnList = "code", unique = true),
            @Index(name = "idx_flight_city_name", columnList = "name")
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

    @Column(nullable = false, length = 2)
    private String countryCode;

    public FlightCityEntity(String code, String name, String countryCode) {
        this.code = code;
        this.name = name;
        this.countryCode = countryCode;
    }
}
