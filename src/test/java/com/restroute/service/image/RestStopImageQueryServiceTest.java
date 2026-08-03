package com.restroute.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.repository.RestStopImageRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.image.exception.RestStopNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopImageQueryServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopImageRepository restStopImageRepository;

    private RestStopImageQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new RestStopImageQueryService(restStopRepository, restStopImageRepository);
    }

    @Test
    @DisplayName("기존 휴게소의 상세 이미지 데이터를 조회한다")
    void findDetailImage_returnsDetailImage() {
        byte[] detailImage = new byte[] {1};
        when(restStopRepository.existsByServiceAreaCode("A00001")).thenReturn(true);
        when(restStopImageRepository.findDetailImageDataByServiceAreaCode("A00001"))
                .thenReturn(Optional.of(detailImage));

        assertThat(queryService.findDetailImage("A00001")).containsSame(detailImage);
    }

    @Test
    @DisplayName("기존 휴게소에 이미지가 없으면 빈 결과를 반환한다")
    void findListImage_returnsEmptyWhenImageIsMissing() {
        when(restStopRepository.existsByServiceAreaCode("A00001")).thenReturn(true);
        when(restStopImageRepository.findListImageDataByServiceAreaCode("A00001"))
                .thenReturn(Optional.empty());

        assertThat(queryService.findListImage("A00001")).isEmpty();
    }

    @Test
    @DisplayName("없는 휴게소의 이미지 조회는 404 예외를 발생시킨다")
    void findDetailImage_throwsWhenRestStopIsMissing() {
        when(restStopRepository.existsByServiceAreaCode("UNKNOWN")).thenReturn(false);

        assertThatThrownBy(() -> queryService.findDetailImage("UNKNOWN")).isInstanceOf(RestStopNotFoundException.class);

        verify(restStopImageRepository, never()).findDetailImageDataByServiceAreaCode("UNKNOWN");
    }

    @Test
    @DisplayName("이미지 행이 있을 때만 상세 이미지 URL을 반환한다")
    void findDetailImageUrl_returnsUrlOnlyWhenImageExists() {
        when(restStopImageRepository.existsById("A00001")).thenReturn(true);
        when(restStopImageRepository.existsById("A00002")).thenReturn(false);

        assertThat(queryService.findDetailImageUrl("A00001")).isEqualTo("/api/rest-stops/A00001/images/detail");
        assertThat(queryService.findDetailImageUrl("A00002")).isNull();
    }

    @Test
    @DisplayName("이미지가 있는 휴게소 코드만 일괄 조회한다")
    void findExistingServiceAreaCodes_returnsImageCodes() {
        List<String> codes = List.of("A00001", "A00002", "A00003");
        when(restStopImageRepository.findServiceAreaCodesIn(codes)).thenReturn(List.of("A00001", "A00003"));

        Set<String> result = queryService.findExistingServiceAreaCodes(codes);

        assertThat(result).containsExactlyInAnyOrder("A00001", "A00003");
        verify(restStopImageRepository).findServiceAreaCodesIn(codes);
    }

    @Test
    @DisplayName("빈 휴게소 코드 목록은 이미지 조회 없이 빈 결과를 반환한다")
    void findExistingServiceAreaCodes_returnsEmptyWithoutQueryForEmptyCodes() {
        Set<String> result = queryService.findExistingServiceAreaCodes(List.of());

        assertThat(result).isEmpty();
        verify(restStopImageRepository, never()).findServiceAreaCodesIn(List.of());
    }
}
