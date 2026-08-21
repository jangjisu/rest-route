package com.restroute.flight.service;

import com.restroute.flight.controller.response.FlightHolidayResponse;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class FlightHolidayQueryService {

    private final HolidayRepository holidayRepository;

    /** months(1~12)가 주어지면 그 달들만, 없으면 전체 공휴일을 날짜 오름차순으로 반환한다. */
    @Transactional(readOnly = true)
    public List<FlightHolidayResponse> findAll(List<Integer> months) {
        List<HolidayEntity> holidays = CollectionUtils.isEmpty(months)
                ? holidayRepository.findAllByOrderByHolidayDateAsc()
                : holidayRepository.findAllByMonthInOrderByHolidayDateAsc(months);
        return holidays.stream().map(FlightHolidayResponse::from).toList();
    }
}
