package com.restroute.oilprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.repository.RestOilPriceRepository;
import com.restroute.oilprice.service.dto.NationalCheapestOilPrice;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestOilPriceRankServiceTest {

    @Mock
    private RestOilPriceRepository restOilPriceRepository;

    private RestOilPriceRankService service;

    private RestOilPriceEntity price(String gasoline, String diesel, String lpg) {
        RestOilPriceEntity entity = mock(RestOilPriceEntity.class);
        lenient().when(entity.getGasolinePrice()).thenReturn(gasoline);
        lenient().when(entity.getDieselPrice()).thenReturn(diesel);
        lenient().when(entity.getLpgPrice()).thenReturn(lpg);
        return entity;
    }

    @Test
    @DisplayName("유종별로 저장된 가격 중 가장 낮은 값을 찾는다")
    void findNationalCheapestPrices_returnsMinimumPerFuelType() {
        service = new RestOilPriceRankService(restOilPriceRepository);
        List<RestOilPriceEntity> all = List.of(
                price("1,850원", "1,900원", "1,135원"),
                price("1,790원", "1,950원", "1,100원"),
                price("1,820원", "1,880원", "1,150원"));
        when(restOilPriceRepository.findAll()).thenReturn(all);

        NationalCheapestOilPrice result = service.findNationalCheapestPrices();

        assertThat(result.gasoline()).isEqualTo(1790);
        assertThat(result.diesel()).isEqualTo(1880);
        assertThat(result.lpg()).isEqualTo(1100);
    }

    @Test
    @DisplayName("가격을 숫자로 해석할 수 없는 행은 제외하고, 전부 못 읽으면 null이다")
    void findNationalCheapestPrices_ignoresUnparsablePrices() {
        service = new RestOilPriceRankService(restOilPriceRepository);
        List<RestOilPriceEntity> all = List.of(price(null, "가격정보없음", ""), price("1,700원", null, null));
        when(restOilPriceRepository.findAll()).thenReturn(all);

        NationalCheapestOilPrice result = service.findNationalCheapestPrices();

        assertThat(result.gasoline()).isEqualTo(1700);
        assertThat(result.diesel()).isNull();
        assertThat(result.lpg()).isNull();
    }

    @Test
    @DisplayName("저장된 가격이 하나도 없으면 세 유종 모두 null이다")
    void findNationalCheapestPrices_returnsAllNullWhenEmpty() {
        service = new RestOilPriceRankService(restOilPriceRepository);
        when(restOilPriceRepository.findAll()).thenReturn(List.of());

        NationalCheapestOilPrice result = service.findNationalCheapestPrices();

        assertThat(result.gasoline()).isNull();
        assertThat(result.diesel()).isNull();
        assertThat(result.lpg()).isNull();
    }
}
