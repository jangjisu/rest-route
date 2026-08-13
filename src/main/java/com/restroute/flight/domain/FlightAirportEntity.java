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
 * 항공권 검색용 공항 참조 데이터 (Travelpayouts /data/ko/airports.json 기준, 취항 공항만).
 * cities.json은 도시 단위 코드만 있어 ICN/KIX 같은 실제 공항코드가 없었는데, 이 엔티티가
 * 그 갭을 메운다. korName/engName 모두 전량 채워진다 — Travelpayouts 자체 커버리지에
 * Wikidata(P238) 보강, 그리고도 남는 나머지는 수동으로 채워 넣었다(자세한 소스/파이프라인은
 * {@code rules/backend/flight.md} 참고).
 */
@Getter
@Entity
@Table(
        name = "flight_airport",
        indexes = {
            @Index(name = "idx_flight_airport_code", columnList = "code", unique = true),
            @Index(name = "idx_flight_airport_kor_name", columnList = "korName"),
            @Index(name = "idx_flight_airport_eng_name", columnList = "engName"),
            @Index(name = "idx_flight_airport_city_code", columnList = "cityCode")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightAirportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column
    private String korName;

    @Column(nullable = false)
    private String engName;

    @Column(nullable = false, length = 3)
    private String cityCode;

    @Column(nullable = false, length = 2)
    private String countryCode;

    public FlightAirportEntity(String code, String korName, String engName, String cityCode, String countryCode) {
        this.code = code;
        this.korName = korName;
        this.engName = engName;
        this.cityCode = cityCode;
        this.countryCode = countryCode;
    }
}
