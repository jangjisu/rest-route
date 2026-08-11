package com.restroute.flight.controller.response;

/**
 * flight 기능 전용 응답 봉투. 나머지 도메인이 쓰는 {@code com.restroute.common.ApiResponse}와는
 * 별개로, flight 쪽만 {data, error} 형태를 쓴다.
 */
public record FlightApiResponse<T>(T data, FlightApiError error) {

    public static <T> FlightApiResponse<T> success(T data) {
        return new FlightApiResponse<>(data, null);
    }

    public static <T> FlightApiResponse<T> error(FlightApiError error) {
        return new FlightApiResponse<>(null, error);
    }
}
