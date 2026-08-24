package com.restroute.reststopcontent.service.admin;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.image.exception.RestStopNotFoundException;
import com.restroute.reststopcontent.controller.request.AdminRestFoodRequest;
import com.restroute.reststopcontent.controller.response.AdminRestFoodResponse;
import com.restroute.reststopcontent.domain.RestFoodEntity;
import com.restroute.reststopcontent.repository.RestFoodImageRepository;
import com.restroute.reststopcontent.repository.RestFoodRepository;
import com.restroute.reststopcontent.service.admin.exception.InvalidRestFoodEditException;
import com.restroute.reststopcontent.service.admin.exception.RestFoodNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminRestFoodServiceTest {

    @Mock
    private RestFoodRepository restFoodRepository;

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestFoodImageRepository restFoodImageRepository;

    private AdminRestFoodService service;

    @BeforeEach
    void setUp() {
        service = new AdminRestFoodService(restFoodRepository, restStopRepository, restFoodImageRepository);
    }

    private AdminRestFoodRequest request() {
        return new AdminRestFoodRequest("커스텀메뉴", "5000", "직접 추가한 메뉴");
    }

    @Test
    @DisplayName("해당 휴게소의 메뉴 목록을 조회한다")
    void findByServiceAreaCode_returnsMenusForRestStop() {
        RestFoodEntity entity = RestFoodEntity.createByAdmin("A00001", "000001", "커스텀메뉴", "5000", "설명");
        when(restFoodRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of(entity));

        List<AdminRestFoodResponse> result = service.findByServiceAreaCode("A00001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).foodName()).isEqualTo("커스텀메뉴");
        assertThat(result.get(0).adminCreated()).isTrue();
    }

    @Test
    @DisplayName("메뉴 목록 조회 시 이미지가 등록된 메뉴만 hasImage가 true다")
    void findByServiceAreaCode_marksHasImageForMenusWithStoredImage() {
        RestFoodEntity withImage = RestFoodEntity.createByAdmin("A00001", "000001", "이미지메뉴", "5000", "설명");
        ReflectionTestUtils.setField(withImage, "id", 1L);
        RestFoodEntity withoutImage = RestFoodEntity.createByAdmin("A00001", "000001", "이미지없는메뉴", "4000", "설명");
        ReflectionTestUtils.setField(withoutImage, "id", 2L);
        when(restFoodRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc("A00001"))
                .thenReturn(List.of(withImage, withoutImage));
        when(restFoodImageRepository.findAllFoodIdsIn(List.of(1L, 2L))).thenReturn(List.of(1L));

        List<AdminRestFoodResponse> result = service.findByServiceAreaCode("A00001");

        assertThat(result).extracting(AdminRestFoodResponse::hasImage).containsExactly(true, false);
    }

    @Test
    @DisplayName("휴게소가 있으면 새 메뉴를 관리자 이름으로 생성해 저장한다")
    void create_savesNewAdminFoodWhenRestStopExists() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restFoodRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminRestFoodResponse result = service.create("A00001", request());

        assertThat(result.foodName()).isEqualTo("커스텀메뉴");
        assertThat(result.adminOverridden()).isTrue();
        assertThat(result.adminCreated()).isTrue();
    }

    @Test
    @DisplayName("휴게소가 없으면 메뉴 생성 시 RestStopNotFoundException을 던진다")
    void create_throwsWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("UNKNOWN", request())).isInstanceOf(RestStopNotFoundException.class);
    }

    @Test
    @DisplayName("기존 메뉴를 수정하면 값이 바뀌고 잠금 상태가 된다")
    void update_appliesEditAndLocksRow() {
        RestFoodEntity existing = RestFoodEntity.createByAdmin("A00001", "000001", "기존메뉴", "3000", "기존설명");
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(existing));

        AdminRestFoodResponse result = service.update("A00001", 1L, request());

        assertThat(result.foodName()).isEqualTo("커스텀메뉴");
        assertThat(result.adminOverridden()).isTrue();
    }

    @Test
    @DisplayName("이미지가 이미 등록된 메뉴를 수정하면 응답에 hasImage가 true로 반영된다")
    void update_reflectsExistingImage() {
        RestFoodEntity existing = RestFoodEntity.createByAdmin("A00001", "000001", "기존메뉴", "3000", "기존설명");
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(existing));
        when(restFoodImageRepository.existsById(1L)).thenReturn(true);

        AdminRestFoodResponse result = service.update("A00001", 1L, request());

        assertThat(result.hasImage()).isTrue();
    }

    @Test
    @DisplayName("메뉴가 없으면 수정 시 RestFoodNotFoundException을 던진다")
    void update_throwsWhenFoodMissing() {
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(99L, "A00001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("A00001", 99L, request()))
                .isInstanceOf(RestFoodNotFoundException.class);
    }

    @Test
    @DisplayName("잠금을 해제하면 다시 동기화 대상이 된다")
    void clearOverride_unlocksRow() {
        RestFoodEntity existing = RestFoodEntity.createByAdmin("A00001", "000001", "기존메뉴", "3000", "기존설명");
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(existing));

        AdminRestFoodResponse result = service.clearOverride("A00001", 1L);

        assertThat(result.adminOverridden()).isFalse();
    }

    @Test
    @DisplayName("관리자가 추가한 메뉴는 삭제할 수 있다")
    void delete_removesAdminCreatedFood() {
        RestFoodEntity existing = RestFoodEntity.createByAdmin("A00001", "000001", "커스텀메뉴", "5000", "설명");
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(existing));

        service.delete("A00001", 1L);

        verify(restFoodRepository).delete(existing);
    }

    @Test
    @DisplayName("동기화로 만들어진 메뉴는 삭제할 수 없다")
    void delete_throwsWhenFoodIsSynced() throws Exception {
        RestFoodEntity synced = RestFoodEntity.from(new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(
                        "{\"stdRestCd\":\"000001\",\"seq\":\"1\",\"foodNm\":\"동기화메뉴\"}",
                        com.restroute.reststopcontent.client.response.RestBestfoodItem.class));
        when(restFoodRepository.findByIdAndRestStopServiceAreaCode(1L, "A00001"))
                .thenReturn(Optional.of(synced));

        assertThatThrownBy(() -> service.delete("A00001", 1L)).isInstanceOf(InvalidRestFoodEditException.class);
    }
}
