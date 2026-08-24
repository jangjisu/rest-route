package com.restroute.holiday.service.admin;

import com.restroute.holiday.controller.request.AdminFlightHolidayRequest;
import com.restroute.holiday.controller.response.AdminFlightHolidayResponse;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import com.restroute.holiday.service.admin.exception.DuplicateFlightHolidayException;
import com.restroute.holiday.service.admin.exception.FlightHolidayNotFoundException;
import com.restroute.holiday.service.admin.exception.InvalidFlightHolidayRequestException;
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

    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public List<AdminFlightHolidayResponse> findAll() {
        return holidayRepository.findAllByOrderByHolidayDateAsc().stream()
                .map(AdminFlightHolidayResponse::from)
                .toList();
    }

    @Transactional
    public AdminFlightHolidayResponse create(AdminFlightHolidayRequest request) {
        LocalDate holidayDate = parseDate(request.date());
        String name = requireName(request.name());
        if (holidayRepository.existsByHolidayDate(holidayDate)) {
            throw DuplicateFlightHolidayException.forDate(holidayDate);
        }
        HolidayEntity saved = holidayRepository.save(HolidayEntity.createdByAdmin(holidayDate, name));
        return AdminFlightHolidayResponse.from(saved);
    }

    @Transactional
    public AdminFlightHolidayResponse delete(Long holidayId) {
        HolidayEntity entity = holidayRepository
                .findById(holidayId)
                .orElseThrow(() -> FlightHolidayNotFoundException.forId(holidayId));
        holidayRepository.delete(entity);
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
