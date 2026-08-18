package com.restroute.flight.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 항공권 검색의 연휴 배지 계산에 쓰는 공휴일 한 건(날짜+이름). 대체공휴일은 매번 새로 지정되기
 * 때문에 고정 시딩 대신, 관리자가 admin 페이지에서 날짜를 클릭해 직접 추가/삭제한다.
 */
@Getter
@Entity
@Table(
        name = "flight_holiday",
        indexes = {@Index(name = "idx_flight_holiday_date", columnList = "holidayDate", unique = true)})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightHolidayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false)
    private String name;

    private FlightHolidayEntity(LocalDate holidayDate, String name) {
        this.holidayDate = holidayDate;
        this.name = name;
    }

    public static FlightHolidayEntity of(LocalDate holidayDate, String name) {
        return new FlightHolidayEntity(holidayDate, name);
    }
}
