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
 * 항공권 검색용 국가 참조 데이터 (Travelpayouts /data/ko/countries.json 기준). korName/engName
 * 둘 다 253개국 전량 채워져 있다.
 */
@Getter
@Entity
@Table(
        name = "flight_country",
        indexes = {
            @Index(name = "idx_flight_country_code", columnList = "code", unique = true),
            @Index(name = "idx_flight_country_kor_name", columnList = "korName"),
            @Index(name = "idx_flight_country_eng_name", columnList = "engName")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightCountryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2)
    private String code;

    @Column(nullable = false)
    private String korName;

    @Column(nullable = false)
    private String engName;

    public FlightCountryEntity(String code, String korName, String engName) {
        this.code = code;
        this.korName = korName;
        this.engName = engName;
    }
}
