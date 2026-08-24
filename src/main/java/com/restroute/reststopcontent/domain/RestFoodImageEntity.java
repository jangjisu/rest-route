package com.restroute.reststopcontent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rest_food_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestFoodImageEntity {

    @Id
    private Long foodId;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] detailImageData;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] listImageData;

    private RestFoodImageEntity(Long foodId, byte[] detailImageData, byte[] listImageData) {
        this.foodId = foodId;
        this.detailImageData = detailImageData;
        this.listImageData = listImageData;
    }

    public static RestFoodImageEntity of(Long foodId, byte[] detailImageData, byte[] listImageData) {
        return new RestFoodImageEntity(foodId, detailImageData, listImageData);
    }
}
