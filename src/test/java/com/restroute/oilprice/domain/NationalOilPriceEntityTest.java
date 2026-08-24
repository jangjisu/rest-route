package com.restroute.oilprice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceItem;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NationalOilPriceEntityTest {

    @Test
    @DisplayName("오피넷 평균가 item을 일별 평균가 entity로 변환한다")
    void from_convertsOpinetItem() throws Exception {
        OpinetAverageOilPriceItem item = new ObjectMapper().readValue("""
                        {
                          "TRADE_DT": "20260707",
                          "PRODCD": "B027",
                          "PRODNM": "휘발유",
                          "PRICE": "1892.88",
                          "DIFF": "-4.19"
                        }
                        """, OpinetAverageOilPriceItem.class);

        NationalOilPriceEntity entity = NationalOilPriceEntity.from(item);

        assertThat(entity.getTradeDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        assertThat(entity.getProductCode()).isEqualTo("B027");
        assertThat(entity.getProductName()).isEqualTo("휘발유");
        assertThat(entity.getPrice()).isEqualTo("1892.88");
        assertThat(entity.getDiff()).isEqualTo("-4.19");
    }

    @Test
    @DisplayName("평균 가격은 반올림한 원 단위 표시값으로 변환한다")
    void formattedPrice_roundsToWon() throws Exception {
        OpinetAverageOilPriceItem item = new ObjectMapper().readValue("""
                        {
                          "TRADE_DT": "20260707",
                          "PRODCD": "B027",
                          "PRODNM": "휘발유",
                          "PRICE": "1892.88",
                          "DIFF": "-4.19"
                        }
                        """, OpinetAverageOilPriceItem.class);

        NationalOilPriceEntity entity = NationalOilPriceEntity.from(item);

        assertThat(entity.formattedPrice()).isEqualTo("1,893원");
        assertThat(entity.roundedPrice()).isEqualTo(1893);
    }
}
