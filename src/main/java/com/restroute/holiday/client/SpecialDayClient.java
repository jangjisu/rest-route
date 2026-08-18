package com.restroute.holiday.client;

import com.restroute.holiday.client.exception.SpecialDayApiException;
import com.restroute.holiday.client.response.SpecialDayResponse;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpecialDayClient {

    private static final String API_NAME = "SPECIAL_DAY";
    private static final int MAX_ROWS = 100;
    private static final int FIRST_PAGE = 1;
    private static final String RESPONSE_TYPE_JSON = "json";

    private final SpecialDayFeignClient specialDayFeignClient;
    private final String serviceKey;

    public SpecialDayClient(
            SpecialDayFeignClient specialDayFeignClient, @Value("${special-day.api.service-key:}") String serviceKey) {
        this.specialDayFeignClient = specialDayFeignClient;
        this.serviceKey = serviceKey;
    }

    /** 그 해의 실제 공휴일(대체공휴일 포함) 원본 항목을 전부 가져온다 — isHoliday 필터링은 호출부 책임이다. */
    public List<SpecialDayResponse.Item> restDaysOfYear(int year) {
        String requestDescription = "getRestDeInfo year=" + year;
        SpecialDayResponse response = fetch(
                requestDescription,
                () -> specialDayFeignClient.getRestDeInfo(
                        String.valueOf(year), MAX_ROWS, FIRST_PAGE, RESPONSE_TYPE_JSON, serviceKey));
        SpecialDayResponse.Header header = response.response().header();
        if (!header.isSuccess()) {
            throw new SpecialDayApiException(requestDescription, header.resultMsg());
        }
        SpecialDayResponse.Body body = response.response().body();
        if (body == null) {
            throw new SpecialDayApiException(requestDescription, "빈 응답");
        }
        return body.itemsOrEmpty();
    }

    private SpecialDayResponse fetch(String requestDescription, Supplier<SpecialDayResponse> request) {
        log.info("External API request started. api={}, endpoint={}", API_NAME, requestDescription);
        try {
            SpecialDayResponse response = request.get();
            if (response == null
                    || response.response() == null
                    || response.response().header() == null) {
                throw new SpecialDayApiException(requestDescription, "빈 응답");
            }
            log.info("External API request succeeded. api={}, endpoint={}", API_NAME, requestDescription);
            return response;
        } catch (SpecialDayApiException e) {
            log.warn(
                    "External API request failed. api={}, endpoint={}, message={}",
                    API_NAME,
                    requestDescription,
                    e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.warn(
                    "External API request failed. api={}, endpoint={}, message={}",
                    API_NAME,
                    requestDescription,
                    e.getMessage());
            throw new SpecialDayApiException(requestDescription, e.getMessage(), e);
        }
    }
}
