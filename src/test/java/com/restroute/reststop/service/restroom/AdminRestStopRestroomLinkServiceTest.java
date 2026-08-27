package com.restroute.reststop.service.restroom;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.reststop.controller.response.AdminRestStopRestroomLinkSummaryResponse;
import com.restroute.reststop.controller.response.AdminRestroomLinkResponse;
import com.restroute.reststop.controller.response.AdminRestroomSearchResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopRestroomEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopRestroomRepository;
import com.restroute.reststop.service.image.exception.RestStopNotFoundException;
import com.restroute.reststop.service.restroom.dto.RestStopRestroomRow;
import com.restroute.reststop.service.restroom.exception.RestStopRestroomNotFoundException;
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
class AdminRestStopRestroomLinkServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopRestroomRepository restStopRestroomRepository;

    private AdminRestStopRestroomLinkService service;

    @BeforeEach
    void setUp() {
        service = new AdminRestStopRestroomLinkService(restStopRepository, restStopRestroomRepository);
    }

    private RestStopRestroomEntity restroomWithId(Long id, String sourceRestStopName) {
        RestStopRestroomEntity restroom =
                RestStopRestroomEntity.from(new RestStopRestroomRow("경부선", sourceRestStopName, "37", "57"));
        ReflectionTestUtils.setField(restroom, "id", id);
        return restroom;
    }

    @Test
    @DisplayName("전체 휴게소마다 연결된 화장실 현황을 함께 반환하고, 연결이 없으면 null이다")
    void findAll_returnsLinkedRestroomPerRestStop() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "죽전(서울)휴게소", "A00001"));
        RestStopEntity unlinkedRestStop = RestStopEntity.from(restStopItem("002", "황간(부산)휴게소", "A00099"));
        RestStopRestroomEntity linkedRestroom = restroomWithId(1L, "죽전(서울)");
        linkedRestroom.updateRestStopServiceAreaCode("A00001");
        when(restStopRepository.findAll()).thenReturn(List.of(restStop, unlinkedRestStop));
        when(restStopRestroomRepository.findAll()).thenReturn(List.of(linkedRestroom));

        List<AdminRestStopRestroomLinkSummaryResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        AdminRestStopRestroomLinkSummaryResponse first = result.get(0);
        assertThat(first.serviceAreaCode()).isEqualTo("A00001");
        assertThat(first.linkedRestroom()).isNotNull();
        assertThat(first.linkedRestroom().sourceRestStopName()).isEqualTo("죽전(서울)");
        AdminRestStopRestroomLinkSummaryResponse second = result.get(1);
        assertThat(second.serviceAreaCode()).isEqualTo("A00099");
        assertThat(second.linkedRestroom()).isNull();
    }

    @Test
    @DisplayName("이름으로 검색하면 이미 연결된 휴게소명을 배치 조회로 함께 반환한다(N+1 방지)")
    void search_returnsMatchesWithLinkedRestStopName() {
        RestStopRestroomEntity linked = restroomWithId(1L, "죽전(서울)");
        linked.updateRestStopServiceAreaCode("A00001");
        RestStopRestroomEntity unlinked = restroomWithId(2L, "죽전(부산)");
        when(restStopRestroomRepository.findAllBySourceRestStopNameContainingIgnoreCaseOrderByIdAsc("죽전"))
                .thenReturn(List.of(linked, unlinked));
        when(restStopRepository.findAllByServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of(RestStopEntity.from(restStopItem("001", "죽전(서울)휴게소", "A00001"))));

        List<AdminRestroomSearchResponse> result = service.search("죽전", null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).linkedRestStopName()).isEqualTo("죽전(서울)휴게소");
        assertThat(result.get(1).linkedRestStopName()).isNull();
        verify(restStopRepository, never()).findByServiceAreaCode(anyString());
    }

    @Test
    @DisplayName("검색 결과에 연결된 휴게소가 하나도 없으면 배치 조회 자체를 하지 않는다")
    void search_skipsBatchLookupWhenNoResultsAreLinked() {
        RestStopRestroomEntity unlinked = restroomWithId(1L, "죽전(부산)");
        when(restStopRestroomRepository.findAllBySourceRestStopNameContainingIgnoreCaseOrderByIdAsc("죽전"))
                .thenReturn(List.of(unlinked));

        List<AdminRestroomSearchResponse> result = service.search("죽전", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).linkedRestStopName()).isNull();
        verify(restStopRepository, never()).findAllByServiceAreaCodeIn(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("노선만 선택하면 해당 노선 전체를 조회한다")
    void search_returnsAllForRouteOnly() {
        RestStopRestroomEntity restroom = restroomWithId(1L, "죽전(서울)");
        when(restStopRestroomRepository.findAllByRouteNameOrderByIdAsc("경부선")).thenReturn(List.of(restroom));

        List<AdminRestroomSearchResponse> result = service.search(null, "경부선");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sourceRestStopName()).isEqualTo("죽전(서울)");
    }

    @Test
    @DisplayName("이름과 노선을 함께 지정하면 둘 다 만족하는 항목만 조회한다")
    void search_returnsMatchesForNameAndRoute() {
        RestStopRestroomEntity restroom = restroomWithId(1L, "죽전(서울)");
        when(restStopRestroomRepository.findAllByRouteNameAndSourceRestStopNameContainingIgnoreCaseOrderByIdAsc(
                        "경부선", "죽전"))
                .thenReturn(List.of(restroom));

        List<AdminRestroomSearchResponse> result = service.search("죽전", "경부선");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("이름과 노선이 모두 비어 있으면 조회하지 않고 빈 목록을 반환한다")
    void search_returnsEmptyWhenBothBlank() {
        List<AdminRestroomSearchResponse> result = service.search(" ", " ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("화장실 현황을 휴게소에 연결하면 해당 휴게소 코드가 반영된다")
    void link_setsRestStopServiceAreaCode() {
        RestStopRestroomEntity restroom = restroomWithId(1L, "죽전(서울)");
        when(restStopRestroomRepository.findById(1L)).thenReturn(Optional.of(restroom));
        when(restStopRepository.findByServiceAreaCode("A00001"))
                .thenReturn(Optional.of(RestStopEntity.from(restStopItem("001", "죽전(서울)휴게소", "A00001"))));

        AdminRestroomLinkResponse result = service.link(1L, "A00001");

        assertThat(result.restStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(result.restStopName()).isEqualTo("죽전(서울)휴게소");
        assertThat(restroom.getRestStopServiceAreaCode()).isEqualTo("A00001");
    }

    @Test
    @DisplayName("연결 대상 휴게소가 없으면 RestStopNotFoundException을 던진다")
    void link_throwsWhenRestStopMissing() {
        RestStopRestroomEntity restroom = restroomWithId(1L, "죽전(서울)");
        when(restStopRestroomRepository.findById(1L)).thenReturn(Optional.of(restroom));
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.link(1L, "UNKNOWN")).isInstanceOf(RestStopNotFoundException.class);
    }

    @Test
    @DisplayName("화장실 현황이 없으면 연결 시 RestStopRestroomNotFoundException을 던진다")
    void link_throwsWhenRestroomMissing() {
        when(restStopRestroomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.link(99L, "A00001")).isInstanceOf(RestStopRestroomNotFoundException.class);
    }

    @Test
    @DisplayName("연결 해제하면 대상 휴게소 코드가 비워진다")
    void unlink_clearsRestStopServiceAreaCode() {
        RestStopRestroomEntity restroom = restroomWithId(1L, "죽전(서울)");
        restroom.updateRestStopServiceAreaCode("A00001");
        when(restStopRestroomRepository.findById(1L)).thenReturn(Optional.of(restroom));

        AdminRestroomLinkResponse result = service.unlink(1L);

        assertThat(result.restStopServiceAreaCode()).isEmpty();
        assertThat(restroom.isUnmapped()).isTrue();
    }

    @Test
    @DisplayName("없는 화장실 현황의 연결 해제는 예외를 던진다")
    void unlink_throwsWhenRestroomMissing() {
        when(restStopRestroomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unlink(99L)).isInstanceOf(RestStopRestroomNotFoundException.class);
    }
}
