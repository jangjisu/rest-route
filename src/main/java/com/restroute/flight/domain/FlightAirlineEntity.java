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
 * 항공권 검색용 항공사 참조 데이터. 인천공항 취항 항공사 API(한국어)와 Travelpayouts
 * airlines.json(영문)을 코드 기준으로 합친다 — 단일 API 응답으로 안 끝나는 병합이라
 * {@code from(Item)} 대신 {@link #of}로 두 값을 직접 받는다. korName은 인천공항이 취항
 * 확인한 109개 항공사만 채워지고(전체의 약 9%), engName은 두 소스 중 하나라도 있으면 항상
 * 채워진다.
 */
@Getter
@Entity
@Table(
        name = "flight_airline",
        indexes = {
            @Index(name = "idx_flight_airline_code", columnList = "code", unique = true),
            @Index(name = "idx_flight_airline_kor_name", columnList = "korName"),
            @Index(name = "idx_flight_airline_eng_name", columnList = "engName")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightAirlineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2)
    private String code;

    @Column
    private String korName;

    @Column(nullable = false)
    private String engName;

    public FlightAirlineEntity(String code, String korName, String engName) {
        this.code = code;
        this.korName = korName;
        this.engName = engName;
    }

    public static FlightAirlineEntity of(String code, String korName, String engName) {
        return new FlightAirlineEntity(code, korName, engName);
    }
}
