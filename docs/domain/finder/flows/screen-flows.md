---
domain: finder
aliases: ["finder 흐름", "위치 팝업", "연료 팝업", "목적지 칩", "후보 팝업"]
paths:
  - "src/main/resources/static/js/finder-entry-flow.js"
  - "src/main/resources/static/js/finder-nearby-search.js"
  - "src/main/resources/static/js/finder-destination-recommendation.js"
  - "src/main/resources/static/js/finder-destination-chips.js"
related_domains: ["place-search-and-map-config", "route"]
sources: []
---

# finder — 사용자·시스템 흐름

**공통 진입**: 랜딩 화면에서 버튼 선택 → 화면별 위치 권한 팝업(`finderPermissionMode1`/
`finderPermissionMode2`) → 연료/EV 관심 팝업(`finderInterestPopup`, 공유) → 각 화면 진입. 위치·연료
둘 다 이미 이번 탭에서 답했으면 팝업 없이 바로 진행한다(상태 수명주기 문서 참고).

**이름·거리로 찾기**: 위치 팝업에서 Allow(좌표 확보) 또는 Skip(좌표 없이 진행) 모두 연료 팝업으로
이어진다 → 진입 시 항상 한 번 조회(위치도 이름도 없으면 서버 호출 없이 빈 목록 유지) → 이름 검색창에
입력할 때마다 재호출(요청 경합은 `finder-rest-stop-nearby-request.js`가 요청 ID로 최신 응답만 반영해
방지) → 서버가 좌표가 있을 때만 거리 오름차순으로 정렬해 내려주므로 프런트는 정렬하지 않는다.

**목적지로 추천받기**: 위치 팝업은 Allow만 있다(실패 시 에러 메시지, 재시도) → 목적지를 정하는 방법이
두 갈래이고, **어느 쪽도 지오코딩을 타지 않는다**:

- **인기 칩**(부산역/대전역/강릉역/광주송정역) 클릭은 후보 선택 없이 바로
  `/api/route-rest-stops/list`를 `destinationName`으로 호출한다. 서버가 그 이름의 좌표를
  `PopularDestination`으로 알고 있어 외부 호출이 길찾기 1회로 끝난다.
- **직접 입력 후 검색**은 먼저 place-search([[place-search-and-map-config]] `GET /api/place-search`)로
  후보 팝업을 띄워 하나를 고른 뒤, 그때 받은 좌표를 `destinationLat`/`destinationLng`로 넘긴다.

→ 조건 필터 칩(규모·이용량은 항상, 나머지 한 자리만 관심 항목에 따라 EV 충전 또는 유가 저렴한 곳)을
선택하면 프런트에서 AND 필터링만 수행한다(서버 재호출 없음).

**휴게소 상세**: 두 화면 모두 결과 카드를 누르면(클릭/Enter/Space) 지도 화면(`index.html`)과 같은 상세
팝업이 finder 화면 안에서 그대로 뜬다(페이지 이동 없음). 닫기 전까지 검색/필터 상태는 유지된다.

**실패/빈 결과**: 이름·거리로 찾기는 검색 결과가 없거나 위치·이름 둘 다 없으면 상태 텍스트만 보여주고
목록을 비운다(별도 에러 코드 없음 — nearby 엔드포인트는 파라미터가 전부 optional이라 항상 200과 빈
배열/부분 필드로 응답). 목적지로 추천받기는 목적지를 못 찾거나 경로 API 실패 시 상태 텍스트로
안내하고, 목적지 후보 팝업은 검색 결과 0건이면 "검색 결과가 없어요", 카카오 API 장애면 "장소 검색을
잠시 이용할 수 없어요"를 보여준다.
