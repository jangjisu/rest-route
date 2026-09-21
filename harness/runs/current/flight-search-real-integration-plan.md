# 항공권 검색 API 실제 연동(Travelpayouts grouped_prices) 계획

## 범위

- `FlightSearchService.fetch()`의 real 분기(현재 `UnsupportedOperationException`)를 실제 구현으로 교체
- `TravelpayoutsClient.groupedPrices(...)` 응답(`TravelpayoutsPriceItem`)을 `FlightDealResponse`로 매핑
- 기존 mock이 이미 갖춘 계약(세션 기반 cursor 페이지네이션, sort, nights 파싱)은 최대한 그대로 재사용 — 새로 만들 건 "실제 데이터를 가져와서 그 계약에 맞는 리스트로 바꾸는 부분"뿐
- `API.md`에 grouped_prices 실측 결과 기록(프로젝트 컨벤션 `rules/api-integration.md` 준수)

## 실측 결과 (2026-08-13, 토큰으로 직접 호출)

| 항목 | 확인된 사실 |
|---|---|
| 응답 단위 | `departure_at`를 `YYYY-MM`로 주면, 그 달의 날짜별 최저가 1건씩(`{"2026-09-01": {...}, ...}`) |
| destination 생략 시 | 날짜마다 최저가인 목적지를 API가 알아서 섞어서 줌 (mock의 "10개 목적지 순환"을 API가 이미 대신 해줌) |
| min/max_trip_duration 미지정 시 | 편도 데이터만 옴(`return_at` 필드 자체가 안 옴, `duration_back:0`) |
| min/max_trip_duration 지정 시 | 그 범위 안에서 가장 싼 왕복 조합 1건만 그 날짜 대표값으로 옴 — nights별로 각각 안 옴 |
| 응답 밀도 | 9월 한 달 기준 8~25건 수준(항공권 인벤토리 자체가 매일 있지 않음) |
| origin/destination | 도시 코드(예: SEL/OSA) |
| origin_airport/destination_airport | 실제 공항 코드(예: ICN/KIX) — FlightAirportEntity가 이미 이 갭을 메우도록 설계됨 |

## 핵심 결론 — mock과의 근본적 차이 (사용자 확인 필요)

지금 mock은 "nights마다, 페이지마다 넉넉하게" 결과를 만들어내지만, 실제 API는 **"그 날짜에 출발하면 최저가가 얼마"인 하루당 1건짜리 목록**이다. 3개월 범위로 검색해도 실제로는 최대 ~90여 건, 보통 그보다 훨씬 적게 나온다. 지금 프론트가 mock을 기준으로 "페이지당 20개씩 여러 페이지"를 기대하고 있다면, 실제 연동에서는 첫 페이지에서 끝나는 경우가 흔할 것이다 — 이건 버그가 아니라 실제 항공권 인벤토리의 특성이다.

## 설계

1. **월 단위 fan-out**: `dateFrom`~`dateTo`가 걸치는 각 달마다 `groupedPrices` 1회씩 호출해서 합친다(최대 3회 — validator가 3개월까지만 허용). 응답엔 그 달 전체가 오므로, `dateFrom`~`dateTo` 범위 밖 날짜는 걸러낸다.
2. **destination**: 있으면 그대로 전달, 없으면 파라미터 자체를 생략(API가 알아서 목적지를 섞어줌 — mock처럼 별도 순환 로직 불필요).
3. **nights**: `request.parsedNights()`의 최솟값/최댓값을 `min_trip_duration`/`max_trip_duration`으로 그대로 전달. 응답의 `departure_at`~`return_at` 차이로 실제 nights를 역산해서 `FlightDealResponse.nights`에 채운다.
4. **destination/origin 표시명**: `destination_airport`로 `FlightAirportRepository.findByCode` 조회(이름 캐시는 이후 제거됨). 지금 `FlightDealResponse.Destination`이 공항코드 하나만 들고 있는데, 실제 데이터엔 도시코드도 같이 오니 그대로 공항코드를 채운다(응답 계약은 안 바꿈).
5. **항공사 한글명**: `item.airline()`(IATA 코드)로 `FlightAirlineRepository.findByCode` 조회.
6. **id 발급**: mock과 동일하게 세션 토큰 + 인덱스 조합 재사용(이미 `FlightDealSessionStore`가 세션 생성 시점에 부여) — 실제 데이터에 안정적인 id가 없어도 문제없음.
7. **정렬**: 이미 `FlightSearchService.fetch()`에 붙여둔 `sorted(..., request.parsedSort())`를 그대로 재사용 — mock/real 공통 경로.
8. **에러 처리**: `TravelpayoutsApiException`을 `FlightExceptionHandler`에 매핑 추가(예: 503 `EXTERNAL_API_ERROR`) — 지금은 처리기가 없어서 그대로 터지면 공용 500으로 샌다.
9. **테스트**: `TravelpayoutsClient`는 인터페이스라 서비스 테스트는 모킹으로 처리. 실제 호출 검증은 이번 계획 문서에 남긴 실측 결과로 갈음하고 반복 호출은 하지 않는다(쿼터/비용).

## 브랜치

새 브랜치(`feature/flight-search-real-integration`) — 직전 PR들(#85~#87)은 이미 머지됐고, 이번 작업은 그와 무관한 새 작업 단위.
