package com.restroute.reststopcontent.service;

import static com.restroute.support.RestStopTestFixtures.restThemeItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.reststopcontent.domain.RestThemeEntity;
import com.restroute.reststopcontent.repository.RestThemeRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestThemeQueryServiceTest {

    @Mock
    private RestThemeRepository restThemeRepository;

    private RestThemeQueryService restThemeQueryService;

    @BeforeEach
    void setUp() {
        restThemeQueryService = new RestThemeQueryService(restThemeRepository);
    }

    @Test
    @DisplayName("테마가 있는 휴게소 코드를 일괄 조회한다")
    void findThemeMappedServiceAreaCodes_returnsMappedCodes() {
        RestThemeEntity theme = RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소"));
        theme.updateRestStopServiceAreaCode("A00001");
        when(restThemeRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of(theme));

        List<String> result = restThemeQueryService.findThemeMappedServiceAreaCodes(List.of("A00001"));

        assertThat(result).containsExactly("A00001");
    }

    @Test
    @DisplayName("한 휴게소에 테마가 여러 개여도 휴게소 코드는 중복하지 않는다")
    void findThemeMappedServiceAreaCodes_removesDuplicateRestStops() {
        RestThemeEntity first = RestThemeEntity.from(restThemeItem("000001", "4계절 꽃이 있는 휴게소"));
        first.updateRestStopServiceAreaCode("A00001");
        RestThemeEntity second = RestThemeEntity.from(restThemeItem("000001", "포토존"));
        second.updateRestStopServiceAreaCode("A00001");
        when(restThemeRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of(first, second));

        assertThat(restThemeQueryService.findThemeMappedServiceAreaCodes(List.of("A00001")))
                .containsExactly("A00001");
    }

    @Test
    @DisplayName("휴게소 코드가 없으면 조회하지 않는다")
    void findThemeMappedServiceAreaCodes_returnsEmptyForBlankInput() {
        assertThat(restThemeQueryService.findThemeMappedServiceAreaCodes(List.of("", " ")))
                .isEmpty();
    }
}
