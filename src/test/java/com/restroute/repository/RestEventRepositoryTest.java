package com.restroute.repository;

import static com.restroute.support.RestStopTestFixtures.restEventItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.domain.RestEventEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestEventRepositoryTest {

    @Autowired
    private RestEventRepository restEventRepository;

    @Test
    @DisplayName("연결된 휴게소 코드 기준으로 이벤트 여러 행을 등록 순서대로 조회한다")
    void findAllByRestStopServiceAreaCodeOrderByIdAsc_returnsRowsInInsertionOrder() {
        RestEventEntity first = RestEventEntity.from(restEventItem("000001", "1665"));
        first.updateRestStopServiceAreaCode("A00001");
        RestEventEntity second = RestEventEntity.from(restEventItem("000001", "3021"));
        second.updateRestStopServiceAreaCode("A00001");
        RestEventEntity other = RestEventEntity.from(restEventItem("000099", "1"));
        other.updateRestStopServiceAreaCode("A00099");
        restEventRepository.saveAll(List.of(first, second, other));

        List<RestEventEntity> result = restEventRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    @DisplayName("연결된 이벤트가 없는 휴게소 코드는 빈 리스트를 반환한다")
    void findAllByRestStopServiceAreaCodeOrderByIdAsc_returnsEmptyWhenNoMatch() {
        RestEventEntity event = RestEventEntity.from(restEventItem("000001", "1665"));
        event.updateRestStopServiceAreaCode("A00001");
        restEventRepository.save(event);

        List<RestEventEntity> result = restEventRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A99999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("휴게소 코드 목록으로 이벤트를 한 번에 조회한다")
    void findAllByRestStopServiceAreaCodeIn_returnsMatchingRows() {
        RestEventEntity matching = RestEventEntity.from(restEventItem("000001", "1665"));
        matching.updateRestStopServiceAreaCode("A00001");
        RestEventEntity other = RestEventEntity.from(restEventItem("000099", "1"));
        other.updateRestStopServiceAreaCode("A00099");
        restEventRepository.saveAll(List.of(matching, other));

        List<RestEventEntity> result =
                restEventRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001", "A00050"));

        assertThat(result).containsExactly(matching);
    }
}
