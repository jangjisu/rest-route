package com.restroute.oilprice.repository;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.oilprice.client.response.RestOilItem;
import com.restroute.oilprice.domain.RestOilEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
class RestOilRepositoryTest {

    @Autowired
    private RestOilRepository restOilRepository;

    @Test
    @DisplayName("노선 코드와 정규화 시설명 기준으로 주유소 편의시설 여러 행을 조회한다")
    void findAllByRouteCodeAndNormalizedStationNameOrderByIdAsc_returnsMatchingRows() {
        RestOilEntity first = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilItem secondItem = restOilItem("000002", "서울만남(부산)주유소");
        ReflectionTestUtils.setField(secondItem, "convenienceName", "세차장");
        RestOilEntity second = RestOilEntity.from(secondItem);
        RestOilItem differentRouteItem = restOilItem("000600", "서울만남(부산)주유소");
        ReflectionTestUtils.setField(differentRouteItem, "routeCode", "9999");
        restOilRepository.saveAll(List.of(first, second, RestOilEntity.from(differentRouteItem)));

        List<RestOilEntity> result =
                restOilRepository.findAllByRouteCodeAndNormalizedStationNameOrderByIdAsc("0010", "서울만남(부산)");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    @DisplayName("표준 주유소 코드 기준으로 같은 물리적 주유소의 편의시설 행 전체를 조회한다")
    void findAllByStandardRestCode_returnsAllFacilityRowsForStation() {
        RestOilEntity first = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        RestOilItem secondItem = restOilItem("000002", "서울만남(부산)주유소");
        ReflectionTestUtils.setField(secondItem, "convenienceName", "세차장");
        RestOilEntity second = RestOilEntity.from(secondItem);
        restOilRepository.saveAll(List.of(first, second, RestOilEntity.from(restOilItem("000006", "기흥(부산)주유소"))));

        List<RestOilEntity> result = restOilRepository.findAllByStandardRestCodeOrderByIdAsc("000002");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    @DisplayName("override=false만 지정하면 관리자가 override하지 않은 편의시설만 조회한다")
    void findByRestStopServiceAreaCodesAndAdminOverridden_excludesOverriddenRowsWhenFalse() {
        RestOilEntity notOverridden = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        notOverridden.updateRestStopServiceAreaCode("A00001");
        RestOilEntity overridden = RestOilEntity.from(restOilItem("000006", "기흥(부산)주유소"));
        overridden.applyAdminLink("A00002");
        restOilRepository.saveAll(List.of(notOverridden, overridden));

        List<RestOilEntity> result = restOilRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false);

        assertThat(result).containsExactly(notOverridden);
    }
}
