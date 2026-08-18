package com.restroute.flight.repository;

import com.restroute.flight.domain.FlightHolidayEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightHolidayRepository extends JpaRepository<FlightHolidayEntity, Long> {

    List<FlightHolidayEntity> findAllByOrderByHolidayDateAsc();

    boolean existsByHolidayDate(LocalDate holidayDate);

    List<FlightHolidayEntity> findAllByHolidayDateBetween(LocalDate from, LocalDate to);
}
