package com.restroute.reststop.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.reststop.repository.RestStopImageRepository;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.image.dto.RestStopImageData;
import com.restroute.reststop.service.image.exception.InvalidRestStopImageException;
import com.restroute.reststop.service.image.exception.RestStopNotFoundException;
import com.restroute.reststop.service.image.util.RestStopImageProcessor;
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
class RestStopImageCommandServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopImageRepository restStopImageRepository;

    @Mock
    private RestStopImageProcessor processor;

    private RestStopImageCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new RestStopImageCommandService(restStopRepository, restStopImageRepository, processor);
    }

    @Test
    @DisplayName("기존 휴게소 이미지를 새 이미지로 저장하거나 교체한다")
    void save_storesProcessedImageForExistingRestStop() {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});
        RestStopImageData data = new RestStopImageData(new byte[] {2}, new byte[] {3});
        when(restStopRepository.existsByServiceAreaCode("A00001")).thenReturn(true);
        when(processor.process(file)).thenReturn(data);

        commandService.save("A00001", file);

        verify(restStopImageRepository).save(any());
    }

    @Test
    @DisplayName("기존 휴게소 이미지 삭제는 이미지가 없어도 멱등적이다")
    void delete_deletesImageForExistingRestStop() {
        when(restStopRepository.existsByServiceAreaCode("A00001")).thenReturn(true);

        commandService.delete("A00001");

        verify(restStopImageRepository).deleteById("A00001");
    }

    @Test
    @DisplayName("없는 휴게소의 이미지 변경은 404 예외를 발생시킨다")
    void save_throwsWhenRestStopIsMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});
        when(restStopRepository.existsByServiceAreaCode("UNKNOWN")).thenReturn(false);

        assertThatThrownBy(() -> commandService.save("UNKNOWN", file)).isInstanceOf(RestStopNotFoundException.class);

        verify(processor, never()).process(any());
        verify(restStopImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미지 변환이 실패하면 저장하지 않는다")
    void save_doesNotPersistWhenProcessingFails() {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});
        when(restStopRepository.existsByServiceAreaCode("A00001")).thenReturn(true);
        when(processor.process(file)).thenThrow(new InvalidRestStopImageException("invalid"));

        assertThatThrownBy(() -> commandService.save("A00001", file)).isInstanceOf(InvalidRestStopImageException.class);

        verify(restStopImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("save/delete는 트랜잭션 경계 안에서 실행된다")
    void save_and_delete_areTransactional() throws NoSuchMethodException {
        assertThat(RestStopImageCommandService.class
                        .getMethod("save", String.class, MultipartFile.class)
                        .isAnnotationPresent(Transactional.class))
                .isTrue();
        assertThat(RestStopImageCommandService.class
                        .getMethod("delete", String.class)
                        .isAnnotationPresent(Transactional.class))
                .isTrue();
    }
}
