package com.restroute.flight.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelpayoutsCityItem(
        String name,
        String code,
        @JsonProperty("country_code") String countryCode,
        @JsonProperty("has_flightable_airport") boolean hasFlightableAirport,
        @JsonProperty("name_translations") NameTranslations nameTranslations) {

    private static final Pattern HANGUL = Pattern.compile("[가-힣]");

    /**
     * name이 실제로 한글일 때만(취항 도시의 약 81%) 반환한다. 나머지는 name이 null이거나
     * 한글이 아닌 로컬 표기(예: "São Jorge")라 korName으로 취급하지 않는다.
     */
    public String korName() {
        return name != null && HANGUL.matcher(name).find() ? name : null;
    }

    /**
     * 영문 번역이 있으면 그걸, 없으면 원본 name(한글이 아니어도 유일하게 있는 표기)을 대신 쓴다.
     * 취항 도시 전량이 둘 중 하나는 갖고 있어서(검증 완료) engName은 사실상 null이 되지 않는다.
     */
    public String engName() {
        String translated = nameTranslations != null ? nameTranslations.en() : null;
        return translated != null ? translated : name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NameTranslations(String en) {}
}
