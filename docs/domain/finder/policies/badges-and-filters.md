---
domain: finder
aliases: ["finder 정책", "배지 정책", "조건 필터", "관심 항목 슬롯"]
paths:
  - "src/main/resources/static/js/finder-condition.js"
  - "src/main/java/com/restroute/reststop/service/RestStopNearbyQueryService.java"
related_domains: ["oil-price", "ev-charger", "route"]
sources: []
---

# finder — 정책과 불변 조건

- **`/api/rest-stops/nearby`는 모든 입력 파라미터가 optional이고, 값이 없을 때 출력 필드를 생략이 아니라
  `null`로 내린다** — 프런트는 "있으면 표시, null/absent면 렌더링 안 함"만 하면 되고 위치·이름·관심
  유무별로 분기하는 별도 코드 경로가 없다.
- **이름·거리로 찾기의 관심 항목은 최대 1개, 마지막 배지 슬롯에만 영향**: 규모/이용량/볼거리/이벤트
  4개 배지는 관심 선택과 무관하게 항상 계산되고, EV 충전 개수 또는 유가 배지 중 하나만 선택한 관심에
  따라 붙는다(`interest`가 `EV`면 EV 카운트만, 연료 종류면 유가 배지만).
- **이름·거리로 찾기의 유가 배지는 "전국 평균보다 저렴"만 판정한다** — 목적지로 추천받기의 "이번 조회
  목록 중 최저가"(CHEAPEST) 같은 집합-내 최저가 판정은 없다(의도적 단순화). 국가 평균 데이터
  ([[oil-price]] `NationalOilPriceService`)가 없으면 값이 없는 것으로 보고 `null`을 반환한다.
- **이름·거리로 찾기의 EV 충전 배지는 개수가 1 이상일 때만 노출**: `evChargerCount`가 `null`이거나
  0이면 배지 자체를 만들지 않는다.
- **먹거리/화장실은 두 화면 배지에서 전부 빠져 있다**: 먹거리는 없는 휴게소가 사실상 없어 DB 누락만으로
  "없음"처럼 보일 위험이 있고, 화장실은 실시간 잔여 좌석수가 아니라 표시 근거가 약하다고 판단해서다.
- **목적지로 추천받기의 배지·필터는 항상 관심 항목당 1개만 켜진다**: 규모/이용량 2개는 관심과 무관하게
  항상 계산되고, 마지막 자리는 `interest === 'EV'`면 EV 충전(대수), 유종이면 유가 등급 하나만 붙는다.
  조건 필터 칩 구성(`destinationConditionFilters`)도 배지와 정확히 대응해서 규모·이용량은 항상, 나머지
  한 자리만 관심 항목에 맞춰 보인다(건너뛰었으면 규모·이용량 칩 2개뿐).
- **"유가 저렴한 곳" 필터 하나가 CHEAPEST/BELOW_AVERAGE를 모두 매칭한다** — "제일 저렴"만 따로 거르는
  필터는 없다(`destinationMatchesFilter`의 `CHEAP_FUEL`).
- **인기 목적지 칩은 place-search 후보 팝업을 타지 않는다**: 서버가 좌표를 아는 이름이라 후보 선택
  없이 바로 route-rest-stops/list를 호출한다. 직접 입력만 후보 팝업을 거친다.
