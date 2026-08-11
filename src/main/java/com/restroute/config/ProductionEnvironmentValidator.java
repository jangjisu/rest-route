package com.restroute.config;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
class ProductionEnvironmentValidator implements InitializingBean {

    private final List<RequiredProperty> requiredProperties;

    ProductionEnvironmentValidator(
            @Value("${ex.api.key:}") String exApiKey,
            @Value("${kakao.rest-api-key:}") String kakaoRestApiKey,
            @Value("${naver.maps.ncp-key-id:}") String naverMapsNcpKeyId,
            @Value("${opinet.api.key:}") String opinetApiKey,
            @Value("${ev.api.key:}") String evApiKey) {
        this.requiredProperties = List.of(
                new RequiredProperty("EX_API_KEY", exApiKey),
                new RequiredProperty("KAKAO_REST_API_KEY", kakaoRestApiKey),
                new RequiredProperty("NAVER_MAPS_NCP_KEY_ID", naverMapsNcpKeyId),
                new RequiredProperty("OPINET_API_KEY", opinetApiKey),
                new RequiredProperty("EV_API_KEY", evApiKey));
    }

    @Override
    public void afterPropertiesSet() {
        List<String> invalidNames = requiredProperties.stream()
                .filter(RequiredProperty::hasInvalidValue)
                .map(RequiredProperty::name)
                .toList();

        if (!invalidNames.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required production environment variables: " + String.join(", ", invalidNames));
        }
    }

    private static boolean isInvalid(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.contains("placeholder")
                || normalized.contains("replacewithreal")
                || "yourapikeyhere".equals(normalized)
                || "yourapikey".equals(normalized)
                || (normalized.startsWith("your") && normalized.endsWith("apikey"))
                || "changeme".equals(normalized);
    }

    private record RequiredProperty(String name, String value) {

        private boolean hasInvalidValue() {
            return isInvalid(value);
        }
    }
}
