package com.restroute.reststopcontent.controller.response;

import com.restroute.reststopcontent.domain.RestEventEntity;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record RestStopEventResponse(List<EventInfo> events) {

    private static final DateTimeFormatter SOURCE_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static RestStopEventResponse from(List<RestEventEntity> events) {
        return new RestStopEventResponse(events.stream().map(EventInfo::from).toList());
    }

    public record EventInfo(String name, String detail, String period) {

        public static EventInfo from(RestEventEntity event) {
            return new EventInfo(event.getEventNm(), event.getEventDetail(), periodOf(event));
        }

        private static String periodOf(RestEventEntity event) {
            LocalDate start = LocalDate.parse(event.getStime(), SOURCE_DATE_FORMAT);
            LocalDate end = LocalDate.parse(event.getEtime(), SOURCE_DATE_FORMAT);
            return DISPLAY_DATE_FORMAT.format(start) + " ~ " + DISPLAY_DATE_FORMAT.format(end);
        }
    }
}
