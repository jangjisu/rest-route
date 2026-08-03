package com.restroute.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.restroute.domain.RestFoodEntity;
import com.restroute.repository.RestFoodImageRepository;
import com.restroute.repository.RestFoodRepository;
import com.restroute.service.admin.exception.RestFoodNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminRestFoodImageQueryServiceTest {

    @Mock
    private RestFoodRepository restFoodRepository;

    @Mock
    private RestFoodImageRepository restFoodImageRepository;

    private AdminRestFoodImageQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new AdminRestFoodImageQueryService(restFoodRepository, restFoodImageRepository);
    }

    @Test
    @DisplayName("존재하는 메뉴에 이미지가 있으면 목록용 이미지 데이터를 반환한다")
    void findListImage_returnsStoredImageForExistingFood() {
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(RestFoodEntity.createByAdmin("A00001", "000001", "메뉴", "5000", "설명")));
        when(restFoodImageRepository.findListImageDataByFoodId(1L)).thenReturn(Optional.of(new byte[] {1, 2}));

        Optional<byte[]> result = queryService.findListImage("A00001", 1L);

        assertThat(result).contains(new byte[] {1, 2});
    }

    @Test
    @DisplayName("존재하는 메뉴에 이미지가 없으면 빈 값을 반환한다")
    void findListImage_returnsEmptyWhenNoImageStored() {
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(RestFoodEntity.createByAdmin("A00001", "000001", "메뉴", "5000", "설명")));
        when(restFoodImageRepository.findListImageDataByFoodId(1L)).thenReturn(Optional.empty());

        Optional<byte[]> result = queryService.findListImage("A00001", 1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("메뉴가 없으면 RestFoodNotFoundException을 던진다")
    void findListImage_throwsWhenFoodMissing() {
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(99L, "A00001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.findListImage("A00001", 99L))
                .isInstanceOf(RestFoodNotFoundException.class);
    }
}
