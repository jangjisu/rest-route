package com.restroute.flight.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelpayoutsCountryItem(
        String code,
        String name,
        @JsonProperty("name_translations") NameTranslations nameTranslations) {

    /** 253개국 전량 영문 번역이 있어서(검증 완료) name 폴백은 실제로 쓰이지 않는다. */
    public String engName() {
        String translated = nameTranslations != null ? nameTranslations.en() : null;
        return translated != null ? translated : name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NameTranslations(String en) {}
}
