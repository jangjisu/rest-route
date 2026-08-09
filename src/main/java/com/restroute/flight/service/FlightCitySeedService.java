package com.restroute.flight.service;

import com.restroute.flight.domain.FlightCityEntity;
import com.restroute.flight.repository.FlightCityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 항공권 검색 폼(출발지/목적지 지역권 필터)에 필요한 최소 도시 참조 데이터를 적재한다.
 * 전체 IATA 도시/공항 데이터베이스 동기화는 별도 배치로 다룰 범위이며, 여기서는
 * 화면에서 바로 쓰이는 대표 도시만 시드 데이터로 넣는다.
 */
@Service
@RequiredArgsConstructor
public class FlightCitySeedService {

    private static final String COUNTRY_CODE_KOREA = "KR";
    private static final String COUNTRY_NAME_KOREA = "대한민국";
    private static final String COUNTRY_CODE_JAPAN = "JP";
    private static final String COUNTRY_NAME_JAPAN = "일본";
    private static final String COUNTRY_CODE_VIETNAM = "VN";
    private static final String COUNTRY_NAME_VIETNAM = "베트남";
    private static final String REGION_KOREA = "KOREA";
    private static final String REGION_JAPAN = "JAPAN";
    private static final String REGION_SOUTHEAST_ASIA = "SOUTHEAST_ASIA";
    private static final String REGION_GUAM_SAIPAN = "GUAM_SAIPAN";

    private static final List<FlightCityEntity> SEED_CITIES = List.of(
            new FlightCityEntity("ICN", "Incheon", "인천", COUNTRY_CODE_KOREA, COUNTRY_NAME_KOREA, REGION_KOREA),
            new FlightCityEntity("GMP", "Gimpo", "김포", COUNTRY_CODE_KOREA, COUNTRY_NAME_KOREA, REGION_KOREA),
            new FlightCityEntity("OSA", "Osaka", "오사카", COUNTRY_CODE_JAPAN, COUNTRY_NAME_JAPAN, REGION_JAPAN),
            new FlightCityEntity("FUK", "Fukuoka", "후쿠오카", COUNTRY_CODE_JAPAN, COUNTRY_NAME_JAPAN, REGION_JAPAN),
            new FlightCityEntity("OKA", "Okinawa", "오키나와", COUNTRY_CODE_JAPAN, COUNTRY_NAME_JAPAN, REGION_JAPAN),
            new FlightCityEntity("TYO", "Tokyo", "도쿄", COUNTRY_CODE_JAPAN, COUNTRY_NAME_JAPAN, REGION_JAPAN),
            new FlightCityEntity("NGO", "Nagoya", "나고야", COUNTRY_CODE_JAPAN, COUNTRY_NAME_JAPAN, REGION_JAPAN),
            new FlightCityEntity("BKK", "Bangkok", "방콕", "TH", "태국", REGION_SOUTHEAST_ASIA),
            new FlightCityEntity(
                    "DAD", "Da Nang", "다낭", COUNTRY_CODE_VIETNAM, COUNTRY_NAME_VIETNAM, REGION_SOUTHEAST_ASIA),
            new FlightCityEntity(
                    "SGN",
                    "Ho Chi Minh City",
                    "호치민",
                    COUNTRY_CODE_VIETNAM,
                    COUNTRY_NAME_VIETNAM,
                    REGION_SOUTHEAST_ASIA),
            new FlightCityEntity(
                    "PQC", "Phu Quoc", "푸꾸옥", COUNTRY_CODE_VIETNAM, COUNTRY_NAME_VIETNAM, REGION_SOUTHEAST_ASIA),
            new FlightCityEntity("GUM", "Guam", "괌", "GU", "괌", REGION_GUAM_SAIPAN),
            new FlightCityEntity("SPN", "Saipan", "사이판", "MP", "사이판", REGION_GUAM_SAIPAN));

    private final FlightCityRepository flightCityRepository;

    @Transactional
    public int seedIfEmpty() {
        if (flightCityRepository.count() > 0) {
            return 0;
        }

        flightCityRepository.saveAll(SEED_CITIES);
        return SEED_CITIES.size();
    }
}
