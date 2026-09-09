---
domain: finder
aliases: ["finder 상태", "sessionStorage", "위치 기억", "요청 경합"]
paths:
  - "src/main/resources/static/js/finder-session-memory.js"
related_domains: []
sources: []
---

# finder — 상태와 데이터 수명주기

- **각 화면의 origin/interest**: 화면별 모듈 안의 JS 변수에만 있는 상태이고, 좌표 자체는 절대
  캐시하지 않는다 — 진입할 때마다 `navigator.geolocation.getCurrentPosition()`을 새로 호출해서 최신
  위치를 받아온다(위치가 바뀌었을 수 있어서다).
- **`finder-session-memory.js`(`sessionStorage`)**: "이 위치 팝업/연료 팝업에 이미 답했다"는 사실만 탭
  세션 동안 기억해서, 같은 탭 안에서 다시 들어갈 때 팝업을 또 띄우지 않는다(새로고침에도 살아남고, 탭을
  닫으면 사라진다 — 다른 탭·다음 방문에는 이어지지 않는다). `finder.locationAnswered.nearby-search`/
  `.destination-recommendation`는 `'granted'`/`'skipped'`, `finder.interest`는 고른 유종/EV 값(건너뛰면
  `'NONE'` 센티널로 "안 답함"과 구분 저장). 재선택 UI는 없고 "첫 화면으로" 뒤로가기로도 초기화되지
  않는다(뒤로가기가 랜딩으로 가는 유일한 경로라, 거기서 초기화하면 매번 다시 물어보게 돼 기억을
  두는 의미가 없어진다). 잘못 골랐을 땐 탭을 닫았다 새로 여는 것뿐이다(sessionStorage 수명 그대로).
  **위치 허용 기억은 두 화면이 공유한다**: 브라우저 권한이 탭 전체에 하나뿐이라, 한쪽에서 실제로
  허용되면 `finder-entry-flow.js`가 두 화면 기억을 모두 `'granted'`로 갱신한다.
- **요청 경합**: 두 화면의 목록 요청 모두 요청 ID/AbortController로 최신 요청만 반영하는 공통 패턴을
  쓴다 — 빠르게 조건을 바꿔가며 검색해도 늦게 도착한 오래된 응답이 화면을 덮어쓰지 않는다.
- **캐시**: 없음. 검색어·위치가 바뀔 때마다 매번 재호출.
