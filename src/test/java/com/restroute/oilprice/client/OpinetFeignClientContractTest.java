package com.restroute.oilprice.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpinetFeignClientContractTest {

    @Test
    @DisplayName("전국 평균가 Feign 응답은 content-type 영향을 피하도록 문자열로 받는다")
    void getAverageOilPrices_returnsStringBody() throws Exception {
        Method method = OpinetFeignClient.class.getMethod("getAverageOilPrices", String.class, String.class);

        assertThat(method.getReturnType()).isEqualTo(String.class);
    }
}
