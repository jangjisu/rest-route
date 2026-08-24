package com.restroute.oilprice.controller.response;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OilInfoResponseTest {

    @Test
    @DisplayName("주유 가격과 주유소 편의시설을 응답으로 변환한다")
    void from_mapsOilPriceAndConveniences() {
        LocalDateTime refreshedAt = LocalDateTime.of(2026, 6, 16, 7, 30);
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"), refreshedAt);
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));

        OilInfoResponse response = OilInfoResponse.from(Optional.of(oilPrice), List.of(convenience));

        assertThat(response.oilCompany()).isEqualTo("AD");
        assertThat(response.gasolinePrice()).isEqualTo("1,999원");
        assertThat(response.dieselPrice()).isEqualTo("1,997원");
        assertThat(response.lpgPrice()).isEqualTo("1,157원");
        assertThat(response.telNo()).isEqualTo("02-573-7430");
        assertThat(response.lastRefreshedAt()).isEqualTo(refreshedAt);
        assertThat(response.oilStationConveniences())
                .containsExactly(new OilStationConvenienceResponse("00:00", "24:00", "쉼터", "고객쉼터"));
    }

    @Test
    @DisplayName("주유 가격이 없으면 가격 필드는 null이고 편의시설 배열은 유지한다")
    void from_returnsNullPriceFieldsWhenOilPriceMissing() {
        RestOilEntity convenience = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));

        OilInfoResponse response = OilInfoResponse.from(Optional.empty(), List.of(convenience));

        assertThat(response.oilCompany()).isNull();
        assertThat(response.gasolinePrice()).isNull();
        assertThat(response.dieselPrice()).isNull();
        assertThat(response.lpgPrice()).isNull();
        assertThat(response.telNo()).isNull();
        assertThat(response.lastRefreshedAt()).isNull();
        assertThat(response.oilStationConveniences())
                .containsExactly(new OilStationConvenienceResponse("00:00", "24:00", "쉼터", "고객쉼터"));
    }
}
