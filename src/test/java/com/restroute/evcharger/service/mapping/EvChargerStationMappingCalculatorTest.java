package com.restroute.evcharger.service.mapping;

import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.evcharger.client.response.EvChargerItem;
import com.restroute.evcharger.domain.EvChargerEntity;
import com.restroute.evcharger.domain.EvChargerStationMappingEntity;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EvChargerStationMappingCalculatorTest {

    private final EvChargerStationMappingCalculator calculator = new EvChargerStationMappingCalculator();

    @Test
    void calculate_matchesNameAddressAndDistance() throws Exception {
        RestStopEntity restStop = restStop("서울만남(부산)휴게소", "A00001");
        RestStopDetailEntity detail = detail("A00001", "서울만남(부산)휴게소", "서울특별시 서초구 양재대로 12길 73-71");
        EvChargerEntity charger = charger("ME1", "서울만남(부산) 휴게소", "서울특별시 서초구 양재대로12길73-71", "37.4600218", "127.0420378");

        List<EvChargerStationMappingEntity> result =
                calculator.calculate(List.of(restStop), List.of(detail), List.of(charger));

        assertThat(result)
                .singleElement()
                .satisfies(mapping ->
                        assertThat(mapping.getRestStopServiceAreaCode()).isEqualTo("A00001"));
    }

    @Test
    void calculate_matchesNameAndDistanceWhenAddressDoesNotMatch() throws Exception {
        RestStopEntity restStop = restStop("서울만남(부산)휴게소", "A00001");
        EvChargerEntity charger = charger("ME1", "서울만남(부산) 휴게소", "전혀 다른 주소", "37.4600218", "127.0420378");

        List<EvChargerStationMappingEntity> result =
                calculator.calculate(List.of(restStop), List.of(), List.of(charger));

        assertThat(result).singleElement();
    }

    @Test
    void calculate_matchesDetailNameAndAddressWhenRestStopNameDiffers() throws Exception {
        RestStopEntity restStop = restStop("다른 휴게소", "A00001");
        RestStopDetailEntity detail = detail("A00001", "서울만남(부산)휴게소", "서울특별시 서초구 양재대로 12길 73-71");
        EvChargerEntity charger = charger("ME1", "서울만남(부산) 휴게소", "서울특별시 서초구 양재대로12길73-71", "37.4600218", "127.0420378");

        List<EvChargerStationMappingEntity> result =
                calculator.calculate(List.of(restStop), List.of(detail), List.of(charger));

        assertThat(result).hasSize(1);
        assertThat(result).singleElement();
    }

    @Test
    void calculate_matchesAddressAndDistanceWhenNameDoesNotMatch() throws Exception {
        RestStopEntity restStop = restStop("다른 휴게소", "A00001");
        RestStopDetailEntity detail = detail("A00001", "다른 휴게소", "서울특별시 서초구 양재대로 12길 73-71");
        EvChargerEntity charger = charger("ME1", "서울만남(부산) 휴게소", "서울특별시 서초구 양재대로12길73-71", "37.4600218", "127.0420378");

        List<EvChargerStationMappingEntity> result =
                calculator.calculate(List.of(restStop), List.of(detail), List.of(charger));

        assertThat(result).singleElement();
    }

    @Test
    void calculate_matchesAddressFromAnyDetailBelongingToRestStop() throws Exception {
        RestStopEntity restStop = restStop("다른 휴게소", "A00001");
        RestStopDetailEntity firstDetail = detail("A00001", "다른 휴게소", "일치하지 않는 주소");
        RestStopDetailEntity secondDetail = detail("A00001", "다른 휴게소", "서울특별시 서초구 양재대로 12길 73-71");
        EvChargerEntity charger = charger("ME1", "서울만남(부산) 휴게소", "서울특별시 서초구 양재대로12길73-71", "37.4600218", "127.0420378");

        assertThat(calculator.calculate(List.of(restStop), List.of(firstDetail, secondDetail), List.of(charger)))
                .singleElement()
                .extracting(EvChargerStationMappingEntity::getRestStopServiceAreaCode)
                .isEqualTo("A00001");
    }

    @Test
    void calculate_usesAddressToDisambiguateSameNameCandidates() throws Exception {
        RestStopEntity first = restStop("서울만남(부산)휴게소", "A00001");
        RestStopEntity second = restStop("서울만남(부산)휴게소", "A00002");
        RestStopDetailEntity firstDetail = detail("A00001", "서울만남(부산)휴게소", "맞는 주소");
        RestStopDetailEntity secondDetail = detail("A00002", "서울만남(부산)휴게소", "다른 주소");
        EvChargerEntity charger = charger("ME1", "서울만남(부산) 휴게소", "맞는 주소", "37.4600218", "127.0420378");

        List<EvChargerStationMappingEntity> result =
                calculator.calculate(List.of(first, second), List.of(firstDetail, secondDetail), List.of(charger));

        assertThat(result)
                .singleElement()
                .extracting(EvChargerStationMappingEntity::getRestStopServiceAreaCode)
                .isEqualTo("A00001");
    }

    @Test
    void calculate_usesFirstMatchingCandidateWhenMultipleCandidatesMatch() throws Exception {
        RestStopEntity first = restStop("서울만남(부산)휴게소", "A00001");
        RestStopEntity second = restStop("서울만남(부산)휴게소", "A00002");
        RestStopDetailEntity firstDetail = detail("A00001", "서울만남(부산)휴게소", "같은 주소");
        RestStopDetailEntity secondDetail = detail("A00002", "서울만남(부산)휴게소", "같은 주소");
        EvChargerEntity charger = charger("ME1", "서울만남(부산) 휴게소", "같은 주소", "37.4600218", "127.0420378");

        assertThat(calculator.calculate(List.of(first, second), List.of(firstDetail, secondDetail), List.of(charger)))
                .singleElement()
                .extracting(EvChargerStationMappingEntity::getRestStopServiceAreaCode)
                .isEqualTo("A00001");
    }

    @Test
    void calculate_usesFirstNameMatchingCandidate() throws Exception {
        RestStopEntity first = restStop("서울만남(부산)휴게소", "A00001");
        RestStopEntity second = restStop("서울만남(부산)휴게소", "A00002");
        EvChargerEntity charger = charger("ME1", "서울만남(부산) 휴게소", "다른 주소", "37.4600218", "127.0420378");

        assertThat(calculator.calculate(List.of(first, second), List.of(), List.of(charger)))
                .singleElement()
                .extracting(EvChargerStationMappingEntity::getRestStopServiceAreaCode)
                .isEqualTo("A00001");
    }

    @Test
    void calculate_usesFirstAddressMatchingCandidate() throws Exception {
        RestStopEntity first = restStop("첫 번째 휴게소", "A00001");
        RestStopEntity second = restStop("두 번째 휴게소", "A00002");
        RestStopDetailEntity firstDetail = detail("A00001", "첫 번째 휴게소", "같은 주소");
        RestStopDetailEntity secondDetail = detail("A00002", "두 번째 휴게소", "같은 주소");
        EvChargerEntity charger = charger("ME1", "다른 충전소", "같은 주소", "37.4600218", "127.0420378");

        assertThat(calculator.calculate(List.of(first, second), List.of(firstDetail, secondDetail), List.of(charger)))
                .singleElement()
                .extracting(EvChargerStationMappingEntity::getRestStopServiceAreaCode)
                .isEqualTo("A00001");
    }

    @Test
    void calculate_ignoresInvalidDuplicateDeletedAndFarChargers() throws Exception {
        RestStopEntity restStop = restStop("서울만남(부산)휴게소", "A00001");
        EvChargerEntity active = charger("ME1", "서울만남(부산) 휴게소", "주소", "37.4600218", "127.0420378");
        EvChargerEntity duplicate = charger("ME1", "서울만남(부산) 휴게소", "주소", "37.4600218", "127.0420378");
        EvChargerEntity deleted = charger("ME2", "서울만남(부산) 휴게소", "주소", "37.4600218", "127.0420378", "Y");
        EvChargerEntity far = charger("ME3", "서울만남(부산) 휴게소", "주소", "35.0", "129.0");
        EvChargerEntity invalid = charger("ME4", "서울만남(부산) 휴게소", "주소", "invalid", "129.0");
        EvChargerEntity blankId = charger("", "서울만남(부산) 휴게소", "주소", "37.4600218", "127.0420378");

        List<EvChargerStationMappingEntity> result = calculator.calculate(
                List.of(restStop), List.of(), List.of(active, duplicate, deleted, far, invalid, blankId));

        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(EvChargerStationMappingEntity::getStatId)
                .isEqualTo("ME1");
    }

    @Test
    void calculate_returnsEmptyWhenNoCandidateMatches() throws Exception {
        RestStopEntity restStop = restStop("서울만남(부산)휴게소", "A00001");
        EvChargerEntity charger = charger("ME1", "전혀 다른 충전소", "전혀 다른 주소", "37.4600218", "127.0420378");

        assertThat(calculator.calculate(List.of(restStop), List.of(), List.of(charger)))
                .isEmpty();
    }

    @Test
    void calculate_ignoresMissingCoordinatesAndUnlinkedDetails() throws Exception {
        RestStopEntity restStop = restStop("서울만남(부산)휴게소", "A00001");
        RestStopEntity invalidRestStop = restStop("서울만남(부산)휴게소", "A00002");
        ReflectionTestUtils.setField(invalidRestStop, "xValue", "invalid");
        RestStopDetailEntity unlinkedDetail = detail("A99999", "서울만남(부산)휴게소", "주소");
        RestStopDetailEntity detailWithoutCode = detail("A99999", "서울만남(부산)휴게소", "주소");
        ReflectionTestUtils.setField(detailWithoutCode, "serviceAreaCode", "");
        EvChargerEntity missingLongitude = charger("ME1", "서울만남(부산) 휴게소", "주소", "37.4600218", "");
        EvChargerEntity missingLatitude = charger("ME2", "다른 충전소", "다른 주소", "", "127.0420378");
        EvChargerEntity noMatch = charger("ME3", "다른 충전소", "다른 주소", "37.4600218", "127.0420378");

        assertThat(calculator.calculate(
                        List.of(restStop, invalidRestStop),
                        List.of(unlinkedDetail, detailWithoutCode),
                        List.of(missingLongitude, missingLatitude, noMatch)))
                .isEmpty();
    }

    @Test
    void calculate_ignoresBlankNameAndAddress() throws Exception {
        RestStopEntity restStop = restStop("서울만남(부산)휴게소", "A00001");
        RestStopEntity blankNameRestStop = restStop("", "A00002");
        RestStopDetailEntity detail = detail("A00001", "서울만남(부산)휴게소", "");
        EvChargerEntity charger = charger("ME1", "", "", "37.4600218", "127.0420378");
        EvChargerEntity nonBlankCharger = charger("ME2", "다른 충전소", "", "37.4600218", "127.0420378");

        assertThat(calculator.calculate(
                        List.of(restStop, blankNameRestStop), List.of(detail), List.of(charger, nonBlankCharger)))
                .isEmpty();
    }

    private RestStopEntity restStop(String name, String serviceAreaCode) {
        return RestStopEntity.from(restStopItem("001", name, serviceAreaCode));
    }

    private RestStopDetailEntity detail(String serviceAreaCode, String name, String address) {
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem(serviceAreaCode, name));
        ReflectionTestUtils.setField(detail, "svarAddr", address);
        return detail;
    }

    private EvChargerEntity charger(String statId, String name, String address, String latitude, String longitude)
            throws Exception {
        return charger(statId, name, address, latitude, longitude, "N");
    }

    private EvChargerEntity charger(
            String statId, String name, String address, String latitude, String longitude, String delYn)
            throws Exception {
        String json = "{\"statId\":\""
                + statId
                + "\",\"statNm\":\""
                + name
                + "\",\"addr\":\""
                + address
                + "\",\"lat\":\""
                + latitude
                + "\",\"lng\":\""
                + longitude
                + "\",\"delYn\":\""
                + delYn
                + "\"}";
        return EvChargerEntity.from(new ObjectMapper().readValue(json, EvChargerItem.class));
    }
}
