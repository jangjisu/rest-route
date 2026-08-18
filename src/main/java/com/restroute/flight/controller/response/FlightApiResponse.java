package com.restroute.flight.controller.response;

/**
 * flight 기능 전용 응답 봉투. 나머지 도메인이 쓰는 {@code com.restroute.common.ApiResponse}와는
 * 별개로, flight 쪽만 {data, meta, error} 형태를 쓴다 — meta는 data 안이 아니라 바깥 형제
 * 필드다. 성공이면 data/meta가 채워지고 error는 null, 실패면 반대로 data/meta가 null이고
 * error만 채워진다.
 */
public record FlightApiResponse<T>(T data, FlightDealSearchMeta meta, FlightApiError error) {

    public static <T> FlightApiResponse<T> success(T data, FlightDealSearchMeta meta) {
        return new FlightApiResponse<>(data, meta, null);
    }

    public static <T> FlightApiResponse<T> error(FlightApiError error) {
        return new FlightApiResponse<>(null, null, error);
    }
}
