package com.restroute.reststop.service.restroom.dto;

public record RestStopRestroomRow(
        String routeName, String sourceRestStopName, String maleToiletCount, String femaleToiletCount) {}
