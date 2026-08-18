package com.restroute.holiday.repository;

import com.restroute.holiday.domain.HolidayEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

    List<HolidayEntity> findAllByOrderByHolidayDateAsc();

    boolean existsByHolidayDate(LocalDate holidayDate);

    List<HolidayEntity> findAllByHolidayDateBetween(LocalDate from, LocalDate to);

    /** 배치 동기화가 예전에 넣어둔(관리자가 직접 등록하지 않은) 행만 — 재동기화 시 삭제 후보를 고를 때 쓴다. */
    List<HolidayEntity> findAllByHolidayDateBetweenAndAdminOverriddenFalse(LocalDate from, LocalDate to);
}
