package com.restroute.evcharger.service.dto;

public record EvChargerSyncResult(
        int totalPageCount, int successfulPageCount, int failedPageCount, int savedItemCount, long uniqueStationCount) {

    public static EvChargerSyncResult skipped() {
        return new EvChargerSyncResult(0, 0, 0, 0, 0);
    }

    public static EvChargerSyncResult failed() {
        return new EvChargerSyncResult(0, 0, 0, 0, 0);
    }

    public static EvChargerSyncResult of(
            int totalPageCount,
            int successfulPageCount,
            int failedPageCount,
            int savedItemCount,
            long uniqueStationCount) {
        return new EvChargerSyncResult(
                totalPageCount, successfulPageCount, failedPageCount, savedItemCount, uniqueStationCount);
    }
}
