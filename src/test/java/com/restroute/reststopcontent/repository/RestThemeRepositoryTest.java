package com.restroute.reststopcontent.repository;

import static com.restroute.support.RestStopTestFixtures.restThemeItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststopcontent.domain.RestThemeEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestThemeRepositoryTest {

    @Autowired
    private RestThemeRepository restThemeRepository;

    @Test
    @DisplayName("연결된 휴게소 코드 기준으로 테마 여러 행을 등록 순서대로 조회한다")
    void findAllByRestStopServiceAreaCodeOrderByIdAsc_returnsRowsInInsertionOrder() {
        RestThemeEntity first = RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소"));
        first.updateRestStopServiceAreaCode("A00001");
        RestThemeEntity second = RestThemeEntity.from(restThemeItem("000001", "포토존"));
        second.updateRestStopServiceAreaCode("A00001");
        RestThemeEntity other = RestThemeEntity.from(restThemeItem("000099", "체육공원"));
        other.updateRestStopServiceAreaCode("A00099");
        restThemeRepository.saveAll(List.of(first, second, other));

        List<RestThemeEntity> result = restThemeRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    @DisplayName("연결된 테마가 없는 휴게소 코드는 빈 리스트를 반환한다")
    void findAllByRestStopServiceAreaCodeOrderByIdAsc_returnsEmptyWhenNoMatch() {
        RestThemeEntity theme = RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소"));
        theme.updateRestStopServiceAreaCode("A00001");
        restThemeRepository.save(theme);

        List<RestThemeEntity> result = restThemeRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A99999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("휴게소 코드 목록으로 테마를 한 번에 조회한다")
    void findAllByRestStopServiceAreaCodeIn_returnsMatchingRows() {
        RestThemeEntity matching = RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소"));
        matching.updateRestStopServiceAreaCode("A00001");
        RestThemeEntity other = RestThemeEntity.from(restThemeItem("000099", "체육공원"));
        other.updateRestStopServiceAreaCode("A00099");
        restThemeRepository.saveAll(List.of(matching, other));

        List<RestThemeEntity> result =
                restThemeRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001", "A00050"));

        assertThat(result).containsExactly(matching);
    }
}
