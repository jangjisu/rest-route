package com.restroute.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.domain.RestFoodEntity;
import com.restroute.repository.RestFoodImageRepository;
import com.restroute.repository.RestFoodRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminRestFoodImageCommandServiceTest {

    @Mock
    private RestFoodRepository restFoodRepository;

    @Mock
    private RestFoodImageRepository restFoodImageRepository;

    @Mock
    private RestStopImageProcessor processor;

    private AdminRestFoodImageCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new AdminRestFoodImageCommandService(restFoodRepository, restFoodImageRepository, processor);
    }

    @Test
    @DisplayName("기존 메뉴의 이미지를 저장하거나 교체한다")
    void save_storesProcessedImageForExistingFood() {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});
        RestStopImageData data = new RestStopImageData(new byte[] {2}, new byte[] {3});
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(RestFoodEntity.createByAdmin("A00001", "000001", "메뉴", "5000", "설명")));
        when(processor.process(file)).thenReturn(data);

        commandService.save("A00001", 1L, file);

        verify(restFoodImageRepository).save(any());
    }

    @Test
    @DisplayName("기존 메뉴 이미지 삭제는 이미지가 없어도 멱등적이다")
    void delete_deletesImageForExistingFood() {
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(RestFoodEntity.createByAdmin("A00001", "000001", "메뉴", "5000", "설명")));

        commandService.delete("A00001", 1L);

        verify(restFoodImageRepository).deleteById(1L);
    }

    @Test
    @DisplayName("없는 메뉴의 이미지 변경은 예외를 발생시킨다")
    void save_throwsWhenFoodIsMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(99L, "A00001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.save("A00001", 99L, file))
                .isInstanceOf(RestFoodNotFoundException.class);

        verify(processor, never()).process(any());
        verify(restFoodImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("save/delete는 트랜잭션 경계 안에서 실행된다")
    void save_and_delete_areTransactional() throws NoSuchMethodException {
        assertThat(AdminRestFoodImageCommandService.class
                        .getMethod("save", String.class, Long.class, MultipartFile.class)
                        .isAnnotationPresent(Transactional.class))
                .isTrue();
        assertThat(AdminRestFoodImageCommandService.class
                        .getMethod("delete", String.class, Long.class)
                        .isAnnotationPresent(Transactional.class))
                .isTrue();
    }
}
