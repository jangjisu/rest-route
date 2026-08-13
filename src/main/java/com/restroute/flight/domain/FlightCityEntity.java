package com.restroute.flight.domain;

import com.restroute.flight.client.response.TravelpayoutsCityItem;
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
 * korName은 취항 도시의 약 81%만 채워지고(원본 데이터의 한글 커버리지 한계), 나머지는 null이다.
 * engName은 항상 채워진다.
 */
@Getter
@Entity
@Table(
        name = "flight_city",
        indexes = {
            @Index(name = "idx_flight_city_code", columnList = "code", unique = true),
            @Index(name = "idx_flight_city_kor_name", columnList = "korName"),
            @Index(name = "idx_flight_city_eng_name", columnList = "engName")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightCityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column
    private String korName;

    @Column(nullable = false)
    private String engName;

    @Column(nullable = false, length = 2)
    private String countryCode;

    public FlightCityEntity(String code, String korName, String engName, String countryCode) {
        this.code = code;
        this.korName = korName;
        this.engName = engName;
        this.countryCode = countryCode;
    }

    public static FlightCityEntity from(TravelpayoutsCityItem item) {
        return new FlightCityEntity(item.code(), item.korName(), item.engName(), item.countryCode());
    }
}
