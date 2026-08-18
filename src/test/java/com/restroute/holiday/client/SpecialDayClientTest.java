package com.restroute.holiday.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.restroute.holiday.client.exception.SpecialDayApiException;
import com.restroute.holiday.client.response.SpecialDayResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpecialDayClientTest {

    @Mock
    private SpecialDayFeignClient specialDayFeignClient;

    private SpecialDayClient specialDayClient;

    @BeforeEach
    void setUp() {
        specialDayClient = new SpecialDayClient(specialDayFeignClient, "test-key");
    }

    private static SpecialDayResponse successResponse(List<SpecialDayResponse.Item> items) {
        return new SpecialDayResponse(new SpecialDayResponse.Response(
                new SpecialDayResponse.Header("00", "OK"),
                new SpecialDayResponse.Body(new SpecialDayResponse.Items(items))));
    }

    @Test
    @DisplayName("restDaysOfYear는 연도/서비스키/json 타입으로 getRestDeInfo를 호출하고 항목 목록을 반환한다")
    void restDaysOfYear_returnsItems() {
        SpecialDayResponse.Item item = new SpecialDayResponse.Item("20260815", "광복절", "Y");
        when(specialDayFeignClient.getRestDeInfo("2026", 100, 1, "json", "test-key"))
                .thenReturn(successResponse(List.of(item)));

        List<SpecialDayResponse.Item> result = specialDayClient.restDaysOfYear(2026);

        assertThat(result).containsExactly(item);
    }

    @Test
    @DisplayName("resultCode가 00이 아니면 SpecialDayApiException을 던진다")
    void restDaysOfYear_throwsWhenResultCodeNotSuccess() {
        SpecialDayResponse failure = new SpecialDayResponse(new SpecialDayResponse.Response(
                new SpecialDayResponse.Header("30", "SERVICE_KEY_IS_NOT_REGISTERED_ERROR"), null));
        when(specialDayFeignClient.getRestDeInfo("2026", 100, 1, "json", "test-key"))
                .thenReturn(failure);

        assertThatThrownBy(() -> specialDayClient.restDaysOfYear(2026))
                .isInstanceOf(SpecialDayApiException.class)
                .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }

    @Test
    @DisplayName("응답이 null이면 SpecialDayApiException을 던진다")
    void restDaysOfYear_throwsOnNullResponse() {
        when(specialDayFeignClient.getRestDeInfo("2026", 100, 1, "json", "test-key"))
                .thenReturn(null);

        assertThatThrownBy(() -> specialDayClient.restDaysOfYear(2026))
                .isInstanceOf(SpecialDayApiException.class)
                .hasMessageContaining("빈 응답");
    }

    @Test
    @DisplayName("호출이 런타임 예외를 던지면 SpecialDayApiException으로 감싼다")
    void restDaysOfYear_wrapsRuntimeException() {
        when(specialDayFeignClient.getRestDeInfo("2026", 100, 1, "json", "test-key"))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> specialDayClient.restDaysOfYear(2026))
                .isInstanceOf(SpecialDayApiException.class)
                .hasMessageContaining("boom");
    }

    @Test
    @DisplayName("resultCode는 성공인데 body가 null이면 NPE 대신 SpecialDayApiException을 던진다")
    void restDaysOfYear_throwsWhenBodyIsNull() {
        SpecialDayResponse response = new SpecialDayResponse(
                new SpecialDayResponse.Response(new SpecialDayResponse.Header("00", "OK"), null));
        when(specialDayFeignClient.getRestDeInfo("2026", 100, 1, "json", "test-key"))
                .thenReturn(response);

        assertThatThrownBy(() -> specialDayClient.restDaysOfYear(2026))
                .isInstanceOf(SpecialDayApiException.class)
                .hasMessageContaining("빈 응답");
    }

    @Test
    @DisplayName("items가 비어있으면(item 리스트가 null이어도) 빈 리스트를 반환한다")
    void restDaysOfYear_returnsEmptyListWhenNoItems() {
        SpecialDayResponse response = new SpecialDayResponse(new SpecialDayResponse.Response(
                new SpecialDayResponse.Header("00", "OK"), new SpecialDayResponse.Body(null)));
        when(specialDayFeignClient.getRestDeInfo("2026", 100, 1, "json", "test-key"))
                .thenReturn(response);

        List<SpecialDayResponse.Item> result = specialDayClient.restDaysOfYear(2026);

        assertThat(result).isEmpty();
    }
}
