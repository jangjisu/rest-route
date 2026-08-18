package com.restroute.holiday.service.dto;

public record HolidaySyncResult(int savedCount, int deletedCount) {

    public static HolidaySyncResult of(int savedCount, int deletedCount) {
        return new HolidaySyncResult(savedCount, deletedCount);
    }
}
