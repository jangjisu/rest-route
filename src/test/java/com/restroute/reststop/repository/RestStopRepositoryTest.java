package com.restroute.reststop.repository;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststop.domain.RestStopEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestStopRepositoryTest {

    @Autowired
    private RestStopRepository restStopRepository;

    @Test
    @DisplayName("휴게소 entity를 저장하고 조회한다")
    void saveAndFindAll_returnsSavedRestStop() {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소")));

        List<RestStopEntity> restStops = restStopRepository.findAll();

        assertThat(restStops).hasSize(1);
        assertThat(restStops.get(0).getUnitCode()).isEqualTo("001");
        assertThat(restStops.get(0).getUnitName()).isEqualTo("서울만남(부산)휴게소");
    }

    @Test
    @DisplayName("serviceAreaCode 기준으로 휴게소 entity를 조회한다")
    void findByServiceAreaCode_returnsSavedRestStop() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        restStopRepository.save(restStop);

        assertThat(restStopRepository.findByServiceAreaCode("A00001")).contains(restStop);
    }

    @Test
    @DisplayName("serviceAreaCode 기준 휴게소 존재 여부를 확인한다")
    void existsByServiceAreaCode_returnsWhetherRestStopExists() {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소")));

        assertThat(restStopRepository.existsByServiceAreaCode("A00001")).isTrue();
        assertThat(restStopRepository.existsByServiceAreaCode("UNKNOWN")).isFalse();
    }

    @Test
    @DisplayName("휴게소명 부분일치·대소문자 무시로 조회한다")
    void findByUnitNameContainingIgnoreCase_returnsMatchingRestStops() {
        restStopRepository.save(RestStopEntity.from(restStopItem("001", "서울만남(서울)휴게소", "A00001")));
        restStopRepository.save(RestStopEntity.from(restStopItem("002", "서울만남(부산)휴게소", "A00002")));
        restStopRepository.save(RestStopEntity.from(restStopItem("003", "죽전휴게소", "A00003")));

        List<RestStopEntity> matches = restStopRepository.findByUnitNameContainingIgnoreCase("서울만남");

        assertThat(matches)
                .hasSize(2)
                .extracting(RestStopEntity::getUnitName)
                .containsExactlyInAnyOrder("서울만남(서울)휴게소", "서울만남(부산)휴게소");
    }
}
