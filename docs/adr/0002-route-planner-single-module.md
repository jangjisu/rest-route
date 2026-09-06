# 출발/도착 선택과 경로 결과 시트는 하나의 route-planner 모듈로 합친다

프론트엔드 아키텍처 리뷰(1번 후보)는 `rest-stops-map.js`(당시 1539줄)를 5개 모듈로 쪼개면서
"출발/도착 선택"과 "경로 결과 시트"를 별도 파일로 나누자고 제안했다. 실제로 나눠보니 두
기능은 상태(선택된 출발/도착 좌표, 지도 클릭 모드 on/off)와 흐름(지도 클릭으로 선택 → 조건이
갖춰지면 자동 경로 요청 → 결과 시트 오픈)을 끊임없이 주고받는 하나의 연속된 사용자 여정이라,
파일을 나누면 그 상태를 끌어올려 공유하거나 콜백을 왕복시키는 접합부만 늘어나고 각 파일은
서로 없이는 이해되지 않았다.

대신 `rest-stops-route-planner.js` 하나로 합쳐서 그 상태와 흐름을 한 클로저 안에 두고,
`initRestStopRoutePlanner(...)`가 노출하는 `{bindRouteSearch, bindRouteMapClick,
initializeMobileCurrentLocationOrigin, isMapClickActive, cancelMapSelection,
openRouteResultModal}`만 외부(mapView, 조립 파일 `rest-stops-map.js`)에 계약으로 드러낸다.
파일 자체는 876줄로 리포트가 가정했던 개별 파일보다 커졌지만, 상태 응집도를 파일 수
최소화보다 우선했다.
