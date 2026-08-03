package com.restroute.repository;

import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.domain.RestStopDetailEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestStopDetailRepositoryTest {

    @Autowired
    private RestStopDetailRepository restStopDetailRepository;

    @Test
    @DisplayName("휴게소 상세 entity를 저장하고 조회한다")
    void saveAndFindAll_returnsSavedRestStopDetail() {
        restStopDetailRepository.save(RestStopDetailEntity.from(restStopDetailItem("A00078", "건천(부산)휴게소")));

        List<RestStopDetailEntity> details = restStopDetailRepository.findAll();

        assertThat(details).hasSize(1);
        assertThat(details.get(0).getServiceAreaCode()).isEqualTo("A00078");
        assertThat(details.get(0).getServiceAreaName()).isEqualTo("건천(부산)휴게소");
    }

    @Test
    @DisplayName("serviceAreaCode 기준으로 휴게소 상세 entity를 단건 조회한다")
    void findByServiceAreaCode_returnsSavedRestStopDetail() {
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00078", "건천(부산)휴게소"));
        restStopDetailRepository.save(detail);

        assertThat(restStopDetailRepository.findByServiceAreaCode("A00078")).contains(detail);
    }

    @Test
    @DisplayName("override=false만 지정하면 관리자가 override하지 않은 상세 정보만 조회한다")
    void findByRestStopServiceAreaCodesAndAdminOverridden_excludesOverriddenRowsWhenFalse() {
        RestStopDetailEntity notOverridden = RestStopDetailEntity.from(restStopDetailItem("A00078", "건천(부산)휴게소"));
        notOverridden.updateRestStopServiceAreaCode("A00001");
        RestStopDetailEntity overridden = RestStopDetailEntity.from(restStopDetailItem("A00099", "죽전휴게소"));
        overridden.updateRestStopServiceAreaCode("A00002");
        overridden.applyAdminEdit("031-000-0000", "브랜드", "0010", "주소", "편의점", "Y", "N");
        restStopDetailRepository.saveAll(List.of(notOverridden, overridden));

        List<RestStopDetailEntity> result =
                restStopDetailRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false);

        assertThat(result).containsExactly(notOverridden);
    }
}
