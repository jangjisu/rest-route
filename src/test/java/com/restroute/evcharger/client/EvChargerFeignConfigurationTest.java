package com.restroute.evcharger.client;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Logger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureJsonTesters
class EvChargerFeignConfigurationTest {

    @Autowired
    private FeignClientProperties feignClientProperties;

    @Test
    @DisplayName("전기차 API Feign Client는 rest-route User-Agent와 JSON Accept를 사용한다")
    void evChargerApi_appliesDefaultHeaders() {
        FeignClientProperties.FeignClientConfiguration configuration =
                feignClientProperties.getConfig().get("ev-charger-api");
        Map<String, Collection<String>> headers = configuration.getDefaultRequestHeaders();

        assertThat(headers)
                .containsEntry("User-Agent", List.of("rest-route"))
                .containsEntry("Accept", List.of("application/json"));
    }

    @Test
    @DisplayName("전기차 API Feign Client는 60초 read timeout과 로그 비활성화를 사용한다")
    void evChargerApi_appliesTimeoutAndLoggerConfiguration() {
        FeignClientProperties.FeignClientConfiguration configuration =
                feignClientProperties.getConfig().get("ev-charger-api");

        assertThat(configuration.getReadTimeout()).isEqualTo(60000);
        assertThat(configuration.getLoggerLevel()).isEqualTo(Logger.Level.NONE);
    }

    @Test
    @DisplayName("Feign 기본 read timeout은 EV API 전용 설정과 분리되어 있다")
    void defaultFeignConfiguration_keepsExistingReadTimeout() {
        FeignClientProperties.FeignClientConfiguration configuration =
                feignClientProperties.getConfig().get("default");

        assertThat(configuration.getReadTimeout()).isEqualTo(10000);
    }
}
