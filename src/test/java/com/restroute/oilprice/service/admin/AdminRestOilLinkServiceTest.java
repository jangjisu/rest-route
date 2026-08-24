package com.restroute.oilprice.service.admin;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.domain.RestStopEntity;
import com.restroute.oilprice.controller.response.AdminOilStationSearchResponse;
import com.restroute.oilprice.controller.response.AdminRestOilLinkSummaryResponse;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.repository.RestOilPriceRepository;
import com.restroute.oilprice.repository.RestOilRepository;
import com.restroute.oilprice.service.admin.exception.RestOilNotFoundException;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.image.exception.RestStopNotFoundException;
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
class AdminRestOilLinkServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestOilRepository restOilRepository;

    @Mock
    private RestOilPriceRepository restOilPriceRepository;

    private AdminRestOilLinkService service;

    @BeforeEach
    void setUp() {
        service = new AdminRestOilLinkService(restStopRepository, restOilRepository, restOilPriceRepository);
    }

    private RestOilPriceEntity oilPriceWithId(Long id, String serviceAreaCode2, String serviceAreaName) {
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem(serviceAreaCode2, serviceAreaName));
        ReflectionTestUtils.setField(oilPrice, "id", id);
        return oilPrice;
    }

    @Test
    @DisplayName("전체 휴게소마다 연결된 주유소를 함께 반환하고, 연결이 없으면 null이다")
    void findAll_returnsLinkedOilStationPerRestStop() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopEntity unlinkedRestStop = RestStopEntity.from(restStopItem("002", "마장휴게소", "A00099"));
        RestOilPriceEntity linkedOilPrice = oilPriceWithId(1L, "000002", "서울만남(부산)주유소");
        linkedOilPrice.updateRestStopServiceAreaCode("A00001");
        RestOilEntity canonicalOil = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        canonicalOil.applyAdminLink("A00001");
        RestOilPriceEntity unmatchedOilPrice = oilPriceWithId(2L, "000006", "미매칭주유소");
        when(restStopRepository.findAll()).thenReturn(List.of(restStop, unlinkedRestStop));
        when(restOilRepository.findAll()).thenReturn(List.of(canonicalOil));
        when(restOilPriceRepository.findAll()).thenReturn(List.of(linkedOilPrice, unmatchedOilPrice));

        List<AdminRestOilLinkSummaryResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        AdminRestOilLinkSummaryResponse first = result.get(0);
        assertThat(first.serviceAreaCode()).isEqualTo("A00001");
        assertThat(first.linkedOilStation()).isNotNull();
        assertThat(first.linkedOilStation().standardRestName()).isEqualTo("서울만남(부산)주유소");
        assertThat(first.linkedOilStation().routeName()).isEqualTo("경부선");
        assertThat(first.linkedOilStation().serviceAreaAddress()).isEqualTo("서울시 서초구 원지동10-16");
        assertThat(first.linkedOilStation().direction()).isEqualTo("부산");
        assertThat(first.linkedOilStation().adminOverridden()).isTrue();
        AdminRestOilLinkSummaryResponse second = result.get(1);
        assertThat(second.serviceAreaCode()).isEqualTo("A00099");
        assertThat(second.linkedOilStation()).isNull();
    }

    @Test
    @DisplayName("같은 휴게소 코드에 연결된 주유소 가격이 두 건이면 나중 조회분을 사용한다")
    void findAll_keepsLastOilPriceWhenServiceAreaCodeIsDuplicated() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestOilPriceEntity first = oilPriceWithId(1L, "000002", "먼저조회된주유소");
        first.updateRestStopServiceAreaCode("A00001");
        RestOilPriceEntity last = oilPriceWithId(2L, "000006", "나중조회된주유소");
        last.updateRestStopServiceAreaCode("A00001");
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));
        when(restOilPriceRepository.findAll()).thenReturn(List.of(first, last));

        List<AdminRestOilLinkSummaryResponse> result = service.findAll();

        assertThat(result)
                .singleElement()
                .satisfies(item ->
                        assertThat(item.linkedOilStation().standardRestName()).isEqualTo("나중조회된주유소"));
    }

    @Test
    @DisplayName("이름으로 주유소를 검색하면 이미 연결된 휴게소명을 배치 조회로 함께 반환한다(N+1 방지)")
    void search_returnsMatchesWithLinkedRestStopName() {
        RestOilPriceEntity linkedOilPrice = oilPriceWithId(1L, "000002", "SK에너지 마장주유소");
        linkedOilPrice.updateRestStopServiceAreaCode("A00099");
        RestOilPriceEntity unlinkedOilPrice = oilPriceWithId(2L, "000006", "SK에너지 마장주유소(하행)");
        when(restOilPriceRepository.findAllByServiceAreaNameContainingIgnoreCaseOrderByIdAsc("마장"))
                .thenReturn(List.of(linkedOilPrice, unlinkedOilPrice));
        when(restStopRepository.findAllByServiceAreaCodeIn(List.of("A00099")))
                .thenReturn(List.of(RestStopEntity.from(restStopItem("002", "마장휴게소", "A00099"))));

        List<AdminOilStationSearchResponse> result = service.search("마장", null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).linkedRestStopName()).isEqualTo("마장휴게소");
        assertThat(result.get(1).linkedRestStopName()).isNull();
        verify(restStopRepository, never()).findByServiceAreaCode(anyString());
    }

    @Test
    @DisplayName("검색 결과에 연결된 휴게소가 하나도 없으면 배치 조회 자체를 하지 않는다")
    void search_skipsBatchLookupWhenNoResultsAreLinked() {
        RestOilPriceEntity unlinkedOilPrice = oilPriceWithId(1L, "000006", "SK에너지 마장주유소(하행)");
        when(restOilPriceRepository.findAllByServiceAreaNameContainingIgnoreCaseOrderByIdAsc("마장"))
                .thenReturn(List.of(unlinkedOilPrice));

        List<AdminOilStationSearchResponse> result = service.search("마장", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).linkedRestStopName()).isNull();
        verify(restStopRepository, never()).findAllByServiceAreaCodeIn(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("노선만 선택하면 해당 노선 전체를 조회한다")
    void search_returnsAllForRouteOnly() {
        RestOilPriceEntity oilPrice = oilPriceWithId(1L, "000002", "SK에너지 마장주유소");
        when(restOilPriceRepository.findAllByRouteNameOrderByIdAsc("경부선")).thenReturn(List.of(oilPrice));

        List<AdminOilStationSearchResponse> result = service.search(null, "경부선");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).standardRestName()).isEqualTo("SK에너지 마장주유소");
    }

    @Test
    @DisplayName("이름과 노선을 함께 지정하면 둘 다 만족하는 항목만 조회한다")
    void search_returnsMatchesForNameAndRoute() {
        RestOilPriceEntity oilPrice = oilPriceWithId(1L, "000002", "SK에너지 마장주유소");
        when(restOilPriceRepository.findAllByRouteNameAndServiceAreaNameContainingIgnoreCaseOrderByIdAsc("경부선", "마장"))
                .thenReturn(List.of(oilPrice));

        List<AdminOilStationSearchResponse> result = service.search("마장", "경부선");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("이름과 노선이 모두 비어 있으면 조회하지 않고 빈 목록을 반환한다")
    void search_returnsEmptyWhenBothBlank() {
        List<AdminOilStationSearchResponse> result = service.search(" ", " ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주유소를 휴게소에 연결하면 잠금 상태가 되고, 같은 물리적 주유소의 rest_oil 편의시설 행도 함께 연결된다")
    void link_setsRestStopAndLocksRowAndCascadesToRestOil() {
        RestOilPriceEntity oilPrice = oilPriceWithId(1L, "000002", "SK에너지 마장주유소");
        when(restOilPriceRepository.findById(1L)).thenReturn(Optional.of(oilPrice));
        when(restStopRepository.findByServiceAreaCode("A00099"))
                .thenReturn(Optional.of(RestStopEntity.from(restStopItem("002", "마장휴게소", "A00099"))));
        RestOilEntity siblingOil = RestOilEntity.from(restOilItem("000002", "SK에너지 마장주유소"));
        when(restOilRepository.findAllByStandardRestCodeOrderByIdAsc("000002")).thenReturn(List.of(siblingOil));

        var result = service.link(1L, "A00099");

        assertThat(result.restStopServiceAreaCode()).isEqualTo("A00099");
        assertThat(result.adminOverridden()).isTrue();
        assertThat(siblingOil.getRestStopServiceAreaCode()).isEqualTo("A00099");
        assertThat(siblingOil.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("표준 주유소 코드가 없는 주유소 가격 정보는 rest_oil로 전파하지 않고 연결만 반영한다")
    void link_skipsCascadeWhenServiceAreaCode2Missing() {
        RestOilPriceEntity oilPrice = oilPriceWithId(1L, "000002", "SK에너지 마장주유소");
        ReflectionTestUtils.setField(oilPrice, "serviceAreaCode2", null);
        when(restOilPriceRepository.findById(1L)).thenReturn(Optional.of(oilPrice));
        when(restStopRepository.findByServiceAreaCode("A00099"))
                .thenReturn(Optional.of(RestStopEntity.from(restStopItem("002", "마장휴게소", "A00099"))));

        var result = service.link(1L, "A00099");

        assertThat(result.restStopServiceAreaCode()).isEqualTo("A00099");
    }

    @Test
    @DisplayName("연결 대상 휴게소가 없으면 RestStopNotFoundException을 던진다")
    void link_throwsWhenRestStopMissing() {
        RestOilPriceEntity oilPrice = oilPriceWithId(1L, "000002", "SK에너지 마장주유소");
        when(restOilPriceRepository.findById(1L)).thenReturn(Optional.of(oilPrice));
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.link(1L, "UNKNOWN")).isInstanceOf(RestStopNotFoundException.class);
    }

    @Test
    @DisplayName("주유소가 없으면 연결 시 RestOilNotFoundException을 던진다")
    void link_throwsWhenOilMissing() {
        when(restOilPriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.link(99L, "A00001")).isInstanceOf(RestOilNotFoundException.class);
    }

    @Test
    @DisplayName("연결 해제하면 대상 휴게소가 비워지고 잠금 상태가 되며, rest_oil 편의시설 행도 함께 해제된다")
    void unlink_clearsRestStopAndLocksRowAndCascadesToRestOil() {
        RestOilPriceEntity oilPrice = oilPriceWithId(1L, "000002", "SK에너지 마장주유소");
        oilPrice.updateRestStopServiceAreaCode("A00099");
        when(restOilPriceRepository.findById(1L)).thenReturn(Optional.of(oilPrice));
        RestOilEntity siblingOil = RestOilEntity.from(restOilItem("000002", "SK에너지 마장주유소"));
        siblingOil.applyAdminLink("A00099");
        when(restOilRepository.findAllByStandardRestCodeOrderByIdAsc("000002")).thenReturn(List.of(siblingOil));

        var result = service.unlink(1L);

        assertThat(result.restStopServiceAreaCode()).isNull();
        assertThat(result.adminOverridden()).isTrue();
        assertThat(siblingOil.getRestStopServiceAreaCode()).isNull();
        assertThat(siblingOil.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("잠금을 해제하면 자동 매칭 대상으로 돌아가며, rest_oil 편의시설 행도 함께 풀린다")
    void clearOverride_unlocksRowAndCascadesToRestOil() {
        RestOilPriceEntity oilPrice = oilPriceWithId(1L, "000002", "SK에너지 마장주유소");
        oilPrice.updateRestStopServiceAreaCode("A00099");
        when(restOilPriceRepository.findById(1L)).thenReturn(Optional.of(oilPrice));
        RestOilEntity siblingOil = RestOilEntity.from(restOilItem("000002", "SK에너지 마장주유소"));
        siblingOil.applyAdminLink("A00099");
        when(restOilRepository.findAllByStandardRestCodeOrderByIdAsc("000002")).thenReturn(List.of(siblingOil));

        var result = service.clearOverride(1L);

        assertThat(result.adminOverridden()).isFalse();
        assertThat(siblingOil.isAdminOverridden()).isFalse();
    }

    @Test
    @DisplayName("없는 주유소의 잠금 해제는 예외를 던진다")
    void clearOverride_throwsWhenOilMissing() {
        when(restOilPriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clearOverride(99L)).isInstanceOf(RestOilNotFoundException.class);
    }
}
