---
domain: flight
aliases: ["Travelpayouts", "Aviasales", "grouped_prices"]
paths:
  - "src/main/java/com/restroute/flight/client/TravelpayoutsFeignClient.java"
  - "src/main/java/com/restroute/flight/client/TravelpayoutsClient.java"
  - "src/main/java/com/restroute/flight/client/TravelpayoutsRateLimitingFeignClient.java"
  - "src/main/java/com/restroute/flight/client/TravelpayoutsFeignConfig.java"
  - "src/main/java/com/restroute/flight/client/exception/TravelpayoutsApiException.java"
  - "src/main/java/com/restroute/flight/client/response/TravelpayoutsGroupedPricesResponse.java"
  - "src/main/java/com/restroute/flight/client/response/TravelpayoutsPriceItem.java"
related_domains: ["holiday"]
sources: []
---

# flight — 외부 시스템과 계약

**Travelpayouts Data API (Aviasales)** — `https://api.travelpayouts.com`
(`travelpayouts.api.url`), 토큰은 `TRAVELPAYOUTS_API_TOKEN` 환경변수.
- 엔드포인트: `GET /aviasales/v3/grouped_prices` — `origin`, `destination`(선택),
  `departure_at`(RANGE는 `yyyy-MM`, FIXED는 정확한 날짜), `return_at`(FIXED만),
  `min_trip_duration`/`max_trip_duration`(RANGE만), `currency=krw`, `token`.
- 응답: `{success, currency, data: {키: PriceItem}}`. `success=false`거나 응답 자체가
  없으면 `TravelpayoutsApiException`(→ 공통 `ExternalApiException`)을 던진다.
- `PriceItem`은 origin/destination/공항코드/가격/항공사/편명/출발·귀국 일시/경유
  횟수(가는 편·오는 편 별도)/총 소요시간/`gate`(예약처)/`link`(예약 링크)를 담는다.
- **Rate limiting**: 모든 호출이 `TravelpayoutsRateLimitingFeignClient`(Feign `Client`
  데코레이터)를 거쳐 `TravelpayoutsRateLimiter.acquire()`로 게이트된다. 기본 한도는
  분당 600건으로 초기화되고, 응답 헤더로 서버 기준 실측치로 계속 재보정된다. 예산이
  바닥이면 재보정될 때까지 블로킹(가상 스레드라 비용이 낮다는 전제).
  `TravelpayoutsFeignConfig`는 의도적으로 `@Configuration`을 안 붙여 이 rate limiter가
  다른 Feign 클라이언트에 전역 적용되지 않게 막는다.

**공공데이터포털 특일 정보 API** — flight이 직접 호출하지 않고 `holiday` 도메인을 거쳐
간접 소비한다. 계약 상세는 `holiday` 도메인 문서 7절 참고.

**인천공항/구 Travelpayouts JSON 클라이언트**는 한때 존재했으나 `d598322`에서 제거되고
SQL 파일 기반 정적 시딩(`FlightReferenceDataSeeder`)으로 대체되었다 — 참조 데이터는 더 이상
런타임에 외부 API를 호출하지 않는다.
