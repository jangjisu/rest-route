package com.restroute.reststop.service.image.dto;

import java.util.Arrays;

public record RestStopImageData(byte[] detailImageData, byte[] listImageData) {

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RestStopImageData that)) {
            return false;
        }
        return Arrays.equals(detailImageData, that.detailImageData) && Arrays.equals(listImageData, that.listImageData);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(detailImageData);
        result = 31 * result + Arrays.hashCode(listImageData);
        return result;
    }

    @Override
    public String toString() {
        return "RestStopImageData[detailImageData=%s, listImageData=%s]"
                .formatted(Arrays.toString(detailImageData), Arrays.toString(listImageData));
    }
}
