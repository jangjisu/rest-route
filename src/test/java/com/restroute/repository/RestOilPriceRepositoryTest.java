package com.restroute.repository;

import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.client.response.RestOilPriceItem;
import com.restroute.domain.RestOilPriceEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
class RestOilPriceRepositoryTest {

    @Autowired
    private RestOilPriceRepository restOilPriceRepository;

    @Test
    @DisplayName("주유소 코드 기준으로 가격 정보를 조회한다")
    void findByServiceAreaCode2_returnsMatchingRow() {
        RestOilPriceEntity matching = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceItem differentItem = restOilPriceItem("000006", "기흥(부산)주유소");
        ReflectionTestUtils.setField(differentItem, "gasolinePrice", "1,990원");
        restOilPriceRepository.saveAll(List.of(matching, RestOilPriceEntity.from(differentItem)));

        RestOilPriceEntity result =
                restOilPriceRepository.findByServiceAreaCode2("000002").orElseThrow();

        assertThat(result).isEqualTo(matching);
        assertThat(result.getGasolinePrice()).isEqualTo("1,999원");
    }

    @Test
    @DisplayName("주유소명으로 대소문자 구분 없이 부분 검색한다")
    void findAllByServiceAreaNameContainingIgnoreCaseOrderByIdAsc_returnsMatchingRows() {
        RestOilPriceEntity matching = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity notMatching = RestOilPriceEntity.from(restOilPriceItem("000006", "기흥(부산)주유소"));
        restOilPriceRepository.saveAll(List.of(matching, notMatching));

        List<RestOilPriceEntity> result =
                restOilPriceRepository.findAllByServiceAreaNameContainingIgnoreCaseOrderByIdAsc("만남");

        assertThat(result).containsExactly(matching);
    }

    @Test
    @DisplayName("노선명으로 전체를 조회한다")
    void findAllByRouteNameOrderByIdAsc_returnsMatchingRows() {
        RestOilPriceEntity matching = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceItem otherRouteItem = restOilPriceItem("000006", "기흥(부산)주유소");
        ReflectionTestUtils.setField(otherRouteItem, "routeName", "중부내륙선");
        restOilPriceRepository.saveAll(List.of(matching, RestOilPriceEntity.from(otherRouteItem)));

        List<RestOilPriceEntity> result = restOilPriceRepository.findAllByRouteNameOrderByIdAsc("경부선");

        assertThat(result).containsExactly(matching);
    }

    @Test
    @DisplayName("노선명과 주유소명으로 함께 조회한다")
    void findAllByRouteNameAndServiceAreaNameContainingIgnoreCaseOrderByIdAsc_returnsMatchingRows() {
        RestOilPriceEntity matching = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceEntity sameRouteDifferentName = RestOilPriceEntity.from(restOilPriceItem("000006", "기흥(부산)주유소"));
        RestOilPriceItem otherRouteSameNameItem = restOilPriceItem("000009", "서울만남(부산)주유소");
        ReflectionTestUtils.setField(otherRouteSameNameItem, "routeName", "중부내륙선");
        restOilPriceRepository.saveAll(
                List.of(matching, sameRouteDifferentName, RestOilPriceEntity.from(otherRouteSameNameItem)));

        List<RestOilPriceEntity> result =
                restOilPriceRepository.findAllByRouteNameAndServiceAreaNameContainingIgnoreCaseOrderByIdAsc(
                        "경부선", "만남");

        assertThat(result).containsExactly(matching);
    }

    @Test
    @DisplayName("override=false만 지정하면 관리자가 override하지 않은 가격 정보만 조회한다")
    void findByRestStopServiceAreaCodesAndAdminOverridden_excludesOverriddenRowsWhenFalse() {
        RestOilPriceEntity notOverridden = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        notOverridden.updateRestStopServiceAreaCode("A00001");
        RestOilPriceEntity overridden = RestOilPriceEntity.from(restOilPriceItem("000006", "기흥(부산)주유소"));
        overridden.applyAdminLink("A00002");
        restOilPriceRepository.saveAll(List.of(notOverridden, overridden));

        List<RestOilPriceEntity> result =
                restOilPriceRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false);

        assertThat(result).containsExactly(notOverridden);
    }
}
