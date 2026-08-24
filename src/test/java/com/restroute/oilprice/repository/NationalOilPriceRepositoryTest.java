package com.restroute.oilprice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceItem;
import com.restroute.oilprice.domain.NationalOilPriceEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class NationalOilPriceRepositoryTest {

    @Autowired
    private NationalOilPriceRepository nationalOilPriceRepository;

    @Test
    @DisplayName("거래일 기준으로 전국 평균 유가를 조회한다")
    void findAllByTradeDate_returnsMatchingRows() throws Exception {
        NationalOilPriceEntity gasoline = entity("20260707", "B027", "휘발유", "1892.88", "-4.19");
        NationalOilPriceEntity diesel = entity("20260706", "D047", "자동차용경유", "1884.59", "-1.10");
        nationalOilPriceRepository.saveAll(List.of(gasoline, diesel));

        List<NationalOilPriceEntity> result = nationalOilPriceRepository.findAllByTradeDate(LocalDate.of(2026, 7, 7));

        assertThat(result).containsExactly(gasoline);
    }

    private NationalOilPriceEntity entity(
            String tradeDate, String productCode, String productName, String price, String diff) throws Exception {
        OpinetAverageOilPriceItem item = new ObjectMapper()
                .readValue(
                        """
                        {
                          "TRADE_DT": "%s",
                          "PRODCD": "%s",
                          "PRODNM": "%s",
                          "PRICE": "%s",
                          "DIFF": "%s"
                        }
                        """.formatted(tradeDate, productCode, productName, price, diff),
                        OpinetAverageOilPriceItem.class);
        return NationalOilPriceEntity.from(item);
    }
}
