package com.restroute.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ExApiFeignConfigurationTest {

    @Autowired
    private FeignClientProperties feignClientProperties;

    @Test
    @DisplayName("EX API Feign Client는 rest-route User-Agent를 기본 헤더로 사용한다")
    void exApi_appliesUserAgentHeader() {
        assertThat(feignClientProperties.getConfig()).containsKey("ex-api");

        FeignClientProperties.FeignClientConfiguration exApiConfiguration =
                feignClientProperties.getConfig().get("ex-api");
        Map<String, Collection<String>> headers = exApiConfiguration.getDefaultRequestHeaders();

        assertThat(headers)
                .containsEntry("User-Agent", List.of("rest-route"))
                .containsEntry("Accept", List.of("application/json"));
    }
}
