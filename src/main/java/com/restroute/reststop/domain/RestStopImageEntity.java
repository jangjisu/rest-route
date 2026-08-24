package com.restroute.reststop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rest_stop_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopImageEntity {

    @Id
    private String serviceAreaCode;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] detailImageData;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] listImageData;

    private RestStopImageEntity(String serviceAreaCode, byte[] detailImageData, byte[] listImageData) {
        this.serviceAreaCode = serviceAreaCode;
        this.detailImageData = detailImageData;
        this.listImageData = listImageData;
    }

    public static RestStopImageEntity of(String serviceAreaCode, byte[] detailImageData, byte[] listImageData) {
        return new RestStopImageEntity(serviceAreaCode, detailImageData, listImageData);
    }
}
