package com.restroute.service.admindashboard.dto;

import com.restroute.admin.domain.AdminActivityLogEntity;
import java.time.format.DateTimeFormatter;

public record AdminActivityLogItemResponse(String actor, String message, String occurredAt) {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminActivityLogItemResponse from(AdminActivityLogEntity entity) {
        return new AdminActivityLogItemResponse(
                entity.getActor(), entity.getMessage(), entity.getCreatedAt().format(DISPLAY_FORMAT));
    }
}
