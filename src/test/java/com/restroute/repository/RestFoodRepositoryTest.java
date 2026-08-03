package com.restroute.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.RestBestfoodItem;
import com.restroute.domain.RestFoodEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestFoodRepositoryTest {

    @Autowired
    private RestFoodRepository restFoodRepository;

    @Test
    @DisplayName("휴게소 코드 기준으로 음식 메뉴 여러 행을 등록 순서대로 조회한다")
    void findAllByStdRestCdOrderByIdAsc_returnsRowsInInsertionOrder() throws Exception {
        RestFoodEntity first = foodEntity("000001", "농심어묵우동");
        RestFoodEntity second = foodEntity("000001", "한우국밥");
        RestFoodEntity other = foodEntity("000099", "돈까스");
        restFoodRepository.saveAll(List.of(first, second, other));

        List<RestFoodEntity> result = restFoodRepository.findAllByStdRestCdOrderByIdAsc("000001");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    @DisplayName("음식 메뉴가 없는 휴게소 코드는 빈 리스트를 반환한다")
    void findAllByStdRestCdOrderByIdAsc_returnsEmptyWhenNoMatch() throws Exception {
        restFoodRepository.save(foodEntity("000001", "농심어묵우동"));

        List<RestFoodEntity> result = restFoodRepository.findAllByStdRestCdOrderByIdAsc("999999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("id와 소속 휴게소 코드가 모두 일치해야 메뉴를 조회한다")
    void findByIdAndRestStopServiceAreaCode_requiresBothIdAndServiceAreaCodeToMatch() throws Exception {
        RestFoodEntity entity = RestFoodEntity.createByAdmin("A00001", "000001", "커스텀메뉴", "5000", "설명");
        restFoodRepository.save(entity);

        Optional<RestFoodEntity> match =
                restFoodRepository.findByIdAndRestStopServiceAreaCode(entity.getId(), "A00001");
        Optional<RestFoodEntity> mismatch =
                restFoodRepository.findByIdAndRestStopServiceAreaCode(entity.getId(), "A99999");

        assertThat(match).contains(entity);
        assertThat(mismatch).isEmpty();
    }

    @Test
    @DisplayName("override=false만 지정하면 관리자가 override하지 않은 메뉴만 조회한다")
    void findByRestStopServiceAreaCodesAndAdminOverridden_excludesOverriddenRowsWhenFalse() throws Exception {
        RestFoodEntity notOverridden = foodEntity("000001", "농심어묵우동");
        notOverridden.updateRestStopServiceAreaCode("A00001");
        RestFoodEntity overridden = foodEntity("000002", "한우국밥");
        overridden.updateRestStopServiceAreaCode("A00002");
        overridden.applyAdminEdit("한우국밥(수정)", "9000", "설명");
        restFoodRepository.saveAll(List.of(notOverridden, overridden));

        List<RestFoodEntity> result = restFoodRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false);

        assertThat(result).containsExactly(notOverridden);
    }

    @Test
    @DisplayName("override 조건이 null이면 override 여부와 상관없이 전부 조회한다")
    void findByRestStopServiceAreaCodesAndAdminOverridden_returnsAllWhenOverriddenIsNull() throws Exception {
        RestFoodEntity notOverridden = foodEntity("000001", "농심어묵우동");
        notOverridden.updateRestStopServiceAreaCode("A00001");
        RestFoodEntity overridden = foodEntity("000002", "한우국밥");
        overridden.updateRestStopServiceAreaCode("A00002");
        overridden.applyAdminEdit("한우국밥(수정)", "9000", "설명");
        restFoodRepository.saveAll(List.of(notOverridden, overridden));

        List<RestFoodEntity> result = restFoodRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, null);

        assertThat(result).containsExactlyInAnyOrder(notOverridden, overridden);
    }

    private RestFoodEntity foodEntity(String stdRestCd, String foodNm) throws Exception {
        String json = """
                {"stdRestCd":"%s","foodNm":"%s","foodCost":"7000","recommendyn":"N"}
                """.formatted(stdRestCd, foodNm);
        RestBestfoodItem item = new ObjectMapper().readValue(json, RestBestfoodItem.class);
        return RestFoodEntity.from(item);
    }
}
