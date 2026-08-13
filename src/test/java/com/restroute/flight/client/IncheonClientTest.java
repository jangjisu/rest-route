package com.restroute.flight.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.exception.IncheonApiException;
import com.restroute.flight.client.response.IncheonAirlineApiResponse;
import com.restroute.flight.client.response.IncheonAirlineItem;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncheonClientTest {

    @Mock
    private IncheonFeignClient incheonFeignClient;

    private IncheonClient incheonClient;

    @BeforeEach
    void setUp() {
        incheonClient = new IncheonClient(incheonFeignClient, "test-key");
    }

    @Test
    @DisplayName("serviceAirlines는 응답의 items를 그대로 반환한다")
    void serviceAirlines_returnsItems() {
        IncheonAirlineApiResponse response = new IncheonAirlineApiResponse(new IncheonAirlineApiResponse.Response(
                new IncheonAirlineApiResponse.Body(List.of(new IncheonAirlineItem("KE", "대한항공")))));
        when(incheonFeignClient.getServiceAirlineInfo(anyString(), anyString(), anyInt()))
                .thenReturn(response);

        List<IncheonAirlineItem> result = incheonClient.serviceAirlines();

        assertThat(result).extracting(IncheonAirlineItem::iataCode).containsExactly("KE");
    }

    @Test
    @DisplayName("응답이 null이면 IncheonApiException을 던진다")
    void serviceAirlines_throwsOnNullResponse() {
        when(incheonFeignClient.getServiceAirlineInfo(anyString(), anyString(), anyInt()))
                .thenReturn(null);

        assertThatThrownBy(() -> incheonClient.serviceAirlines())
                .isInstanceOf(IncheonApiException.class)
                .hasMessageContaining("빈 응답");
    }

    @Test
    @DisplayName("호출이 런타임 예외를 던지면 IncheonApiException으로 감싼다")
    void serviceAirlines_wrapsRuntimeException() {
        when(incheonFeignClient.getServiceAirlineInfo(anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> incheonClient.serviceAirlines())
                .isInstanceOf(IncheonApiException.class)
                .hasMessageContaining("boom");
    }
}
