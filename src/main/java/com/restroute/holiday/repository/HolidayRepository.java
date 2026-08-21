package com.restroute.holiday.repository;

import com.restroute.holiday.domain.HolidayEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

    List<HolidayEntity> findAllByOrderByHolidayDateAsc();

    boolean existsByHolidayDate(LocalDate holidayDate);

    /** 배치 동기화가 예전에 넣어둔(관리자가 직접 등록하지 않은) 행만 — 재동기화 시 삭제 후보를 고를 때 쓴다. */
    List<HolidayEntity> findAllByHolidayDateBetweenAndAdminOverriddenFalse(LocalDate from, LocalDate to);

    /** 그 기간의 공휴일 전체(이름 포함, 관리자 등록분 포함) — 연차 배지 계산이 날짜별 이름까지 필요할 때 쓴다. */
    List<HolidayEntity> findAllByHolidayDateBetween(LocalDate from, LocalDate to);

    /** 그 해에 이미 저장된 날짜 전체(관리자 등록분 포함) — 신규 저장 후보를 거를 때 건별 조회 대신 한 번에 쓴다. */
    @Query("select h.holidayDate from HolidayEntity h where h.holidayDate between :from and :to")
    List<LocalDate> findHolidayDatesBetween(LocalDate from, LocalDate to);

    /** 월(1~12) 값이 주어진 목록에 속하는 공휴일만 날짜 오름차순으로 반환한다(연도 무관). */
    @Query("select h from HolidayEntity h where month(h.holidayDate) in :months order by h.holidayDate asc")
    List<HolidayEntity> findAllByMonthInOrderByHolidayDateAsc(List<Integer> months);
}
