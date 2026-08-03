package com.restroute.controller.response;

import java.util.List;

public record RestStopCompareResponse(
        RestStopCompareSide sideA, RestStopCompareSide sideB, RestStopCompareResult result) {

    public static RestStopCompareResponse of(
            RestStopCompareSide sideA, RestStopCompareSide sideB, RestStopCompareResult result) {
        return new RestStopCompareResponse(sideA, sideB, result);
    }

    public record RestStopCompareSide(
            String serviceAreaCode,
            String unitName,
            String routeName,
            String listImageUrl,
            String gasolinePrice,
            String dieselPrice,
            String lpgPrice,
            Integer parkingCount,
            List<String> facilities) {

        public static RestStopCompareSide of(
                String serviceAreaCode,
                String unitName,
                String routeName,
                String listImageUrl,
                String gasolinePrice,
                String dieselPrice,
                String lpgPrice,
                Integer parkingCount,
                List<String> facilities) {
            return new RestStopCompareSide(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    listImageUrl,
                    gasolinePrice,
                    dieselPrice,
                    lpgPrice,
                    parkingCount,
                    List.copyOf(facilities));
        }
    }

    public record RestStopCompareResult(
            String gasolineWinner,
            String dieselWinner,
            String lpgWinner,
            String parkingWinner,
            String facilityWinner,
            String recommendedSide) {

        public static RestStopCompareResult of(
                String gasolineWinner,
                String dieselWinner,
                String lpgWinner,
                String parkingWinner,
                String facilityWinner,
                String recommendedSide) {
            return new RestStopCompareResult(
                    gasolineWinner, dieselWinner, lpgWinner, parkingWinner, facilityWinner, recommendedSide);
        }
    }
}
