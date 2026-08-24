package com.restroute.reststopcontent.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststopcontent.domain.RestFoodImageEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestFoodImageRepositoryTest {

    @Autowired
    private RestFoodImageRepository restFoodImageRepository;

    @Test
    @DisplayName("메뉴 이미지를 저장하고 id로 다시 조회한다")
    void save_thenFindByIdReturnsStoredImage() {
        byte[] detail = {1, 2, 3};
        byte[] list = {4, 5, 6};
        restFoodImageRepository.save(RestFoodImageEntity.of(1L, detail, list));

        assertThat(restFoodImageRepository.findById(1L)).isPresent();
        assertThat(restFoodImageRepository.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("id로 메뉴 이미지를 삭제할 수 있다")
    void deleteById_removesStoredImage() {
        restFoodImageRepository.save(RestFoodImageEntity.of(1L, new byte[] {1}, new byte[] {2}));

        restFoodImageRepository.deleteById(1L);

        assertThat(restFoodImageRepository.existsById(1L)).isFalse();
    }

    @Test
    @DisplayName("조회 대상 id 중 실제로 이미지가 있는 id만 반환한다")
    void findAllFoodIdsIn_returnsOnlyExistingIds() {
        restFoodImageRepository.save(RestFoodImageEntity.of(1L, new byte[] {1}, new byte[] {2}));
        restFoodImageRepository.save(RestFoodImageEntity.of(3L, new byte[] {1}, new byte[] {2}));

        List<Long> found = restFoodImageRepository.findAllFoodIdsIn(List.of(1L, 2L, 3L));

        assertThat(found).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("foodId로 목록용 이미지 데이터를 조회한다")
    void findListImageDataByFoodId_returnsStoredListImage() {
        restFoodImageRepository.save(RestFoodImageEntity.of(1L, new byte[] {1, 2}, new byte[] {3, 4}));

        Optional<byte[]> found = restFoodImageRepository.findListImageDataByFoodId(1L);

        assertThat(found).contains(new byte[] {3, 4});
    }

    @Test
    @DisplayName("이미지가 없는 foodId는 빈 값을 반환한다")
    void findListImageDataByFoodId_returnsEmptyWhenMissing() {
        Optional<byte[]> found = restFoodImageRepository.findListImageDataByFoodId(99L);

        assertThat(found).isEmpty();
    }
}
