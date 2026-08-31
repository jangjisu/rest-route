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
 * {@code from(Item)} 대신 {@link #of}로 두 값을 직접 받는다. korName/engName은 전량
 * 채워져 있다(소스·파이프라인은 {@code rules/backend/flight.md} 참고).
 *
 * <p>{@code isLowCost}는 Travelpayouts {@code /data/airlines.json}의 {@code is_lowcost}
 * 필드를 코드 기준으로 그대로 가져온 값이다 — 그 소스에 없는 코드는 저비용 여부를 알 수
 * 없다는 뜻으로 기본값 {@code false}를 쓴다.
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

    @Column(nullable = false)
    private boolean isLowCost;

    public FlightAirlineEntity(String code, String korName, String engName, boolean isLowCost) {
        this.code = code;
        this.korName = korName;
        this.engName = engName;
        this.isLowCost = isLowCost;
    }

    public static FlightAirlineEntity of(String code, String korName, String engName, boolean isLowCost) {
        return new FlightAirlineEntity(code, korName, engName, isLowCost);
    }
}
