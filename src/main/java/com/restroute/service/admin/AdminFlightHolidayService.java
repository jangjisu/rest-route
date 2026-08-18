package com.restroute.service.admin;

import com.restroute.controller.request.AdminFlightHolidayRequest;
import com.restroute.controller.response.AdminFlightHolidayResponse;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import com.restroute.service.admin.exception.DuplicateFlightHolidayException;
import com.restroute.service.admin.exception.FlightHolidayNotFoundException;
import com.restroute.service.admin.exception.InvalidFlightHolidayRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminFlightHolidayService {

    private final HolidayRepository flightHolidayRepository;

    @Transactional(readOnly = true)
    public List<AdminFlightHolidayResponse> findAll() {
        return flightHolidayRepository.findAllByOrderByHolidayDateAsc().stream()
                .map(AdminFlightHolidayResponse::from)
                .toList();
    }

    @Transactional
    public AdminFlightHolidayResponse create(AdminFlightHolidayRequest request) {
        LocalDate holidayDate = parseDate(request.date());
        String name = requireName(request.name());
        if (flightHolidayRepository.existsByHolidayDate(holidayDate)) {
            throw DuplicateFlightHolidayException.forDate(holidayDate);
        }
        HolidayEntity saved = flightHolidayRepository.save(HolidayEntity.of(holidayDate, name));
        return AdminFlightHolidayResponse.from(saved);
    }

    @Transactional
    public AdminFlightHolidayResponse delete(Long holidayId) {
        HolidayEntity entity = flightHolidayRepository
                .findById(holidayId)
                .orElseThrow(() -> FlightHolidayNotFoundException.forId(holidayId));
        flightHolidayRepository.delete(entity);
        return AdminFlightHolidayResponse.from(entity);
    }

    private static LocalDate parseDate(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            throw InvalidFlightHolidayRequestException.blankDate();
        }
        try {
            return LocalDate.parse(rawDate);
        } catch (DateTimeParseException e) {
            throw InvalidFlightHolidayRequestException.invalidDate(rawDate, e);
        }
    }

    private static String requireName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw InvalidFlightHolidayRequestException.blankName();
        }
        return rawName.trim();
    }
}
