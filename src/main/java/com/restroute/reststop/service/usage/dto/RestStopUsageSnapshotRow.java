package com.restroute.reststop.service.usage.dto;

public record RestStopUsageSnapshotRow(
        String routeName,
        String sourceRestStopName,
        String siteAreaSqm,
        String operationType,
        String dailyVisitorCount,
        String dailyTrafficVolume) {}
