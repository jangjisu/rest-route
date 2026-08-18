package com.restroute.flight.controller;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightApiResponse;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import com.restroute.flight.service.FlightSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 개발용 모킹 컨트롤러. 요청/응답 계약은 실제 grouped_prices 연동 기준으로 확정된
 * 것이지만, 응답 데이터 자체는 아직 고정 가짜 데이터(FlightSearchMockFixture)다 — 실제
 * Travelpayouts 연동은 별도 작업이다. 프론트 개발이 운영 환경에서도 이 계약으로 붙어야 해서
 * 프로파일 제한 없이 항상 활성화된다.
 *
 * <p>쿼리 파라미터는 {@link FlightSearchRequestDto}로 바로 바인딩된다 — 그 DTO의 생성자가
 * 검증까지 끝내기 때문에, 이 메서드에 진입한 시점엔 이미 유효한 검색 조건(cursor/size 포함)이
 * 보장된다(검증 실패는 DTO를 만드는 과정에서 예외로 끝나서 서비스까지 가지 않는다). totalSize는
 * 모킹 전용이라 그 DTO에 넣지 않고 별도 파라미터로 받는다 — 그래서 이 메서드가 받는 값은 실질적으로
 * request(DTO)와 totalSize 둘뿐이다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights")
public class FlightSearchMockController {

    private static final int DEFAULT_TOTAL_SIZE = 77;
    private static final int MIN_TOTAL_SIZE = 1;
    private static final int MAX_TOTAL_SIZE = 1000;

    private final FlightSearchService flightSearchService;

    /**
     * @param request 검색 조건(origin/searchMode/dateFrom/dateTo/destination/nights/sector/includeWeekend/
     *     includeHoliday/includeTransfer/adults/children/infants/sort/locale/currency)과
     *     페이지네이션(cursor/limit)을 쿼리 파라미터로부터 바인딩받은 DTO. 유효하지 않으면
     *     {@link FlightExceptionHandler}가 처리한다.
     * @param totalSize 이번 검색에서 총 몇 건 생성할지(모킹 전용, 옵션, 기본 77, 최대 1000 — limit처럼 범위만
     *     넘으면 조용히 잘라내고 별도 에러는 없음). totalSize는 실제 API 계약에 없는 모킹 전용 값이라
     *     DTO에 넣지 않고 여기서만 클램핑한다.
     */
    @GetMapping("/search/mock")
    public ResponseEntity<FlightApiResponse<List<FlightDealResponse>>> searchMock(
            @ModelAttribute FlightSearchRequestDto request,
            @RequestParam(required = false, defaultValue = "" + DEFAULT_TOTAL_SIZE) int totalSize) {
        int boundedTotalSize = Math.min(Math.max(totalSize, MIN_TOTAL_SIZE), MAX_TOTAL_SIZE);

        FlightDealSearchResponse result = flightSearchService.search(request, boundedTotalSize);
        return ResponseEntity.ok(FlightApiResponse.success(result.items(), result.meta()));
    }
}
