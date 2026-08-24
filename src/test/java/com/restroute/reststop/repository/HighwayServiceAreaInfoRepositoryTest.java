package com.restroute.reststop.repository;

import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststop.client.response.HighwayServiceAreaInfoItem;
import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
class HighwayServiceAreaInfoRepositoryTest {

    @Autowired
    private HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;

    @Test
    @DisplayName("고속도로 휴게소 정보 entity를 저장하고 조회한다")
    void saveAndFindAll_returnsSavedHighwayServiceAreaInfo() {
        highwayServiceAreaInfoRepository.save(
                HighwayServiceAreaInfoEntity.from(highwayServiceAreaInfoItem("000561", "북대전(논산)졸음쉼터")));

        List<HighwayServiceAreaInfoEntity> infos = highwayServiceAreaInfoRepository.findAll();

        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).getServiceAreaCode()).isEqualTo("000561");
        assertThat(infos.get(0).getServiceAreaName()).isEqualTo("북대전(논산)졸음쉼터");
    }

    @Test
    @DisplayName("businessFacilityCode 기준으로 고속도로 휴게소 정보 entity 목록을 조회한다")
    void findAllByBusinessFacilityCode_returnsSavedHighwayServiceAreaInfos() {
        HighwayServiceAreaInfoItem item = highwayServiceAreaInfoItem("000561", "북대전(논산)졸음쉼터");
        ReflectionTestUtils.setField(item, "businessFacilityCode", "A00282");
        HighwayServiceAreaInfoEntity info = HighwayServiceAreaInfoEntity.from(item);
        highwayServiceAreaInfoRepository.save(info);

        List<HighwayServiceAreaInfoEntity> infos =
                highwayServiceAreaInfoRepository.findAllByBusinessFacilityCode("A00282");

        assertThat(infos).containsExactly(info);
    }
}
