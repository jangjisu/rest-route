package com.restroute.holiday.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 항공권 검색의 연휴 배지 계산에 쓰는 공휴일 한 건(날짜+이름). 대체공휴일은 매번 새로 지정되기
 * 때문에 고정 시딩 대신, 매일 특일 정보 API로 동기화하고 관리자가 admin 페이지에서 직접
 * 추가/삭제도 할 수 있다.
 *
 * <p>{@code adminOverridden}은 이 행을 관리자가 직접 등록했는지를 나타낸다 — 배치 동기화는
 * {@code adminOverridden=false}인(=자기가 예전에 넣은) 행만 갱신 대상으로 보고, 관리자가 직접
 * 등록한 행은 API 응답에서 사라져도 절대 지우지 않는다.
 */
@Getter
@Entity
@Table(
        name = "flight_holiday",
        indexes = {@Index(name = "idx_flight_holiday_date", columnList = "holidayDate", unique = true)})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HolidayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean adminOverridden;

    private HolidayEntity(LocalDate holidayDate, String name, boolean adminOverridden) {
        this.holidayDate = holidayDate;
        this.name = name;
        this.adminOverridden = adminOverridden;
    }

    /** 관리자가 admin 페이지에서 직접 등록한 행. 배치 동기화가 절대 지우지 않는다. */
    public static HolidayEntity createdByAdmin(LocalDate holidayDate, String name) {
        return new HolidayEntity(holidayDate, name, true);
    }

    /** 특일 정보 API 동기화가 채워 넣은 행. API 응답에서 더 이상 안 보이면 배치가 지울 수 있다. */
    public static HolidayEntity syncedFromApi(LocalDate holidayDate, String name) {
        return new HolidayEntity(holidayDate, name, false);
    }

    /**
     * 토요일/일요일인지. 이 테이블엔 주말에 걸리는 공휴일도 저장될 수 있다(연차 배지 계산이 그
     * 이름까지 필요해서) — 이 메서드는 저장 여부를 거르는 용도가 아니라, 딜의 출발일이 주말인지
     * 판정하는 용도({@link com.restroute.flight.service.FlightDealPostFilter})로 쓰인다.
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
