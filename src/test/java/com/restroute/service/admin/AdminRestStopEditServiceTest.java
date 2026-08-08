package com.restroute.service.admin;

import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.controller.request.AdminRestStopUpdateRequest;
import com.restroute.controller.response.AdminRestStopEditableResponse;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopDetailRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.admin.exception.InvalidRestStopEditException;
import com.restroute.service.image.exception.RestStopNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminRestStopEditServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopDetailRepository restStopDetailRepository;

    private AdminRestStopEditService service;

    @BeforeEach
    void setUp() {
        service = new AdminRestStopEditService(restStopRepository, restStopDetailRepository);
    }

    private AdminRestStopUpdateRequest validRequest() {
        return new AdminRestStopUpdateRequest(
                "수정된이름", "9999", "수정된노선", "128.0", "38.0", "031-000-0000", "수정브랜드", "9998", "수정주소", "샤워실", "O", "O");
    }

    @Test
    @DisplayName("휴게소와 상세 정보가 모두 있으면 편집 가능한 응답을 반환한다")
    void findEditable_returnsResponseWhenBothExist() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopDetailRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(detail));

        Optional<AdminRestStopEditableResponse> result = service.findEditable("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().serviceAreaCode()).isEqualTo("A00001");
        assertThat(result.get().unitName()).isEqualTo("서울만남(부산)휴게소");
        assertThat(result.get().telNo()).isEqualTo("054-751-6890");
        assertThat(result.get().adminOverridden()).isFalse();
    }

    @Test
    @DisplayName("휴게소 기본 정보가 없으면 빈 결과를 반환한다")
    void findEditable_returnsEmptyWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<AdminRestStopEditableResponse> result = service.findEditable("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("둘 중 하나만 잠겨 있어도 편집 가능 응답은 잠긴 것으로 표시한다")
    void findEditable_reportsOverriddenWhenOnlyDetailIsLocked() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        detail.applyAdminEdit("031-000-0000", "수정브랜드", "9998", "수정주소", "샤워실", "O", "O");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopDetailRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(detail));

        Optional<AdminRestStopEditableResponse> result = service.findEditable("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().adminOverridden()).isTrue();
    }

    @Test
    @DisplayName("휴게소 상세 정보가 없으면 상세 필드가 비어있는 응답을 반환한다")
    void findEditable_returnsResponseWithBlankDetailWhenDetailMissing() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopDetailRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.empty());

        Optional<AdminRestStopEditableResponse> result = service.findEditable("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().unitName()).isEqualTo("서울만남(부산)휴게소");
        assertThat(result.get().telNo()).isNull();
        assertThat(result.get().adminOverridden()).isFalse();
    }

    @Test
    @DisplayName("정상 요청으로 저장하면 두 엔티티 모두 잠기고 수정된 값을 반환한다")
    void update_appliesEditAndLocksBothEntities() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopDetailRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(detail));

        AdminRestStopEditableResponse result = service.update("A00001", validRequest());

        assertThat(result.unitName()).isEqualTo("수정된이름");
        assertThat(result.telNo()).isEqualTo("031-000-0000");
        assertThat(result.adminOverridden()).isTrue();
        assertThat(restStop.isAdminOverridden()).isTrue();
        assertThat(detail.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("휴게소가 없으면 저장 시 RestStopNotFoundException을 던진다")
    void update_throwsWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("UNKNOWN", validRequest()))
                .isInstanceOf(RestStopNotFoundException.class);
    }

    @Test
    @DisplayName("상세 정보가 없으면 저장 시 새로 만들어 잠긴 상태로 저장한다")
    void update_createsDetailWhenMissing() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopDetailRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.empty());
        when(restStopDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminRestStopEditableResponse result = service.update("A00001", validRequest());

        assertThat(result.telNo()).isEqualTo("031-000-0000");
        assertThat(result.adminOverridden()).isTrue();
        ArgumentCaptor<RestStopDetailEntity> savedDetail = ArgumentCaptor.forClass(RestStopDetailEntity.class);
        verify(restStopDetailRepository).save(savedDetail.capture());
        assertThat(savedDetail.getValue().getServiceAreaCode()).isEqualTo("A00001");
        assertThat(savedDetail.getValue().getTelNo()).isEqualTo("031-000-0000");
        assertThat(savedDetail.getValue().isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("좌표가 숫자로 파싱되지 않으면 InvalidRestStopEditException을 던진다")
    void update_throwsWhenCoordinateIsNotParseable() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopDetailRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(detail));
        AdminRestStopUpdateRequest invalidRequest = new AdminRestStopUpdateRequest(
                "수정된이름", "9999", "수정된노선", "숫자아님", "38.0", "031-000-0000", "수정브랜드", "9998", "수정주소", "샤워실", "O", "O");

        assertThatThrownBy(() -> service.update("A00001", invalidRequest))
                .isInstanceOf(InvalidRestStopEditException.class);
    }

    @Test
    @DisplayName("좌표 값이 비어 있으면 검증을 건너뛴다")
    void update_allowsBlankCoordinate() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopDetailRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(detail));
        AdminRestStopUpdateRequest blankCoordinateRequest = new AdminRestStopUpdateRequest(
                "수정된이름", "9999", "수정된노선", "", null, "031-000-0000", "수정브랜드", "9998", "수정주소", "샤워실", "O", "O");

        AdminRestStopEditableResponse result = service.update("A00001", blankCoordinateRequest);

        assertThat(result.xValue()).isEmpty();
        assertThat(result.yValue()).isNull();
    }

    @Test
    @DisplayName("잠금 해제하면 두 엔티티 모두 동기화 잠금이 풀린 상태로 반환한다")
    void clearOverride_unlocksBothEntities() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopDetailEntity detail = RestStopDetailEntity.from(restStopDetailItem("A00001", "서울만남(부산)휴게소"));
        restStop.applyAdminEdit("수정된이름", "9999", "수정된노선", "128.0", "38.0");
        detail.applyAdminEdit("031-000-0000", "수정브랜드", "9998", "수정주소", "샤워실", "O", "O");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopDetailRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(detail));

        AdminRestStopEditableResponse result = service.clearOverride("A00001");

        assertThat(result.adminOverridden()).isFalse();
        assertThat(restStop.isAdminOverridden()).isFalse();
        assertThat(detail.isAdminOverridden()).isFalse();
    }

    @Test
    @DisplayName("휴게소가 없으면 잠금 해제 시 RestStopNotFoundException을 던진다")
    void clearOverride_throwsWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clearOverride("UNKNOWN")).isInstanceOf(RestStopNotFoundException.class);
    }

    @Test
    @DisplayName("신규 등록하면 휴게소와 상세 정보를 저장하고 잠긴 상태로 반환한다")
    void create_savesNewRestStopAndDetailBothLocked() {
        when(restStopRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(restStopDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminRestStopEditableResponse result = service.create(validRequest());

        assertThat(result.unitName()).isEqualTo("수정된이름");
        assertThat(result.telNo()).isEqualTo("031-000-0000");
        assertThat(result.adminOverridden()).isTrue();
        ArgumentCaptor<RestStopEntity> savedRestStop = ArgumentCaptor.forClass(RestStopEntity.class);
        verify(restStopRepository).save(savedRestStop.capture());
        assertThat(savedRestStop.getValue().getServiceAreaCode()).startsWith("ADMIN-");
        ArgumentCaptor<RestStopDetailEntity> savedDetail = ArgumentCaptor.forClass(RestStopDetailEntity.class);
        verify(restStopDetailRepository).save(savedDetail.capture());
        assertThat(savedDetail.getValue().getServiceAreaCode())
                .isEqualTo(savedRestStop.getValue().getServiceAreaCode());
    }

    @Test
    @DisplayName("신규 등록 시 좌표가 숫자로 파싱되지 않으면 InvalidRestStopEditException을 던진다")
    void create_throwsWhenCoordinateIsNotParseable() {
        AdminRestStopUpdateRequest invalidRequest = new AdminRestStopUpdateRequest(
                "새휴게소", "9999", "새노선", "숫자아님", "38.0", "031-000-0000", "새브랜드", "9998", "새주소", "샤워실", "O", "O");

        assertThatThrownBy(() -> service.create(invalidRequest)).isInstanceOf(InvalidRestStopEditException.class);
    }
}
