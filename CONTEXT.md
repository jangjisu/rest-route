# rest-route

고속도로/국도 휴게소 경로 탐색 및 관리자용 데이터 편집 시스템. 카카오 길찾기 API로 경로를
받아 그 위에 있는 휴게소를 찾고, 각 휴게소의 편의시설/주유/음식/테마/이벤트 정보를 조합해
보여준다.

이 문서는 이 프로젝트에서만 쓰이는 용어를 정의한다. 일반적인 프로그래밍 개념(타임아웃,
에러 타입 등)은 여기 포함하지 않는다. 용어가 여러 개 존재할 때는 하나를 고르고 나머지는
`_피해야 할 표현_`에 적는다.

## 언어

**휴게소(Rest Stop)**:
경로 위에서 조회 대상이 되는 시설 하나. `RestStopEntity`로 표현되며, 이름/노선/방향 같은
기본정보를 가진다.
_피해야 할 표현_: 서비스지역, 지점

**서비스지역코드(Service Area Code)**:
휴게소 및 그와 연관된 모든 도메인 테이블(주유/음식/상세/테마/이벤트/EV차저/이미지)을
연결하는 조인 키. 모든 `rest_*` 테이블이 이 코드로 하나의 휴게소를 가리킨다.
_피해야 할 표현_: 지점코드, 매장코드

**관리자 재정의(Admin Override)**:
`adminOverridden` 불리언 필드. `RestOilEntity`/`RestOilPriceEntity`/`RestFoodEntity`/
`RestStopEntity`/`RestStopDetailEntity` 각각에 독립적으로 존재하며, 관리자가 특정 행의
서비스지역코드나 값을 수동으로 지정했음을 뜻한다. 이 값이 true인 행은 자동 매칭(backfill)
대상에서 제외된다. 다만 이 제외 로직은 각 도메인 테이블마다 별도로 구현되어 있고,
전부 동일하게 검사하지는 않는다(아래 백필 참고).
_피해야 할 표현_: 수동 매핑, 락(lock)

**백필(Backfill)**:
아직 서비스지역코드가 채워지지 않은 행에 대해, 이름/노선 등으로 자동 매칭을 시도해
서비스지역코드를 채워 넣는 배치 작업. `RestStopServiceAreaCodeBackfillService`가
9개의 도메인별 backfiller를 조율한다. 도메인마다 관리자 재정의를 검사하는지 여부가
다르다 — 현재 `RestOilServiceAreaCodeBackfiller`/`RestOilPriceServiceAreaCodeBackfiller`
2개만 검사한다.
_피해야 할 표현_: 동기화(sync, 이는 외부 API에서 데이터를 가져오는 별도 작업을 가리킴)

**휴게소 집계(Rest Stop Aggregate)**:
`RestStopAggregateQueryService`가 반환하는, 이미 알려진 서비스지역코드 집합에 대해
연관 정보(상세/주유/음식/테마/이벤트/EV차저/이미지 존재 여부)를 한 번에 조합한 결과.
서비스지역코드가 아직 정해지지 않은 상태에서 "찾는" 용도가 아니라, 이미 정해진 코드에
대해 "읽는" 용도다.
_피해야 할 표현_: 전체 조회, 통합 조회

**경로 위 후보(Route Rest Stop Candidate)**:
카카오 길찾기 API가 반환한 경로 좌표(polyline) 주변 반경 안에 있어, 경로 탐색 응답에
포함될 가능성이 있는 휴게소. 아직 관련 정보(집계)가 조합되기 전 단계.

**방향 그룹(Direction Group)**:
같은 노선의 상행/하행처럼, 물리적으로 다른 휴게소이지만 사용자에게는 "같은 자리의
대안"으로 보여줘야 하는 후보 묶음.

**소통 상태(Nearby Traffic)**:
카카오 길찾기 API의 `road_details`가 제공하는 구간 정체 수준(`trafficState`)을,
휴게소 근처 좌표 기준으로 매핑한 값. `NearbyTrafficStatus`로 표현.

**전국 평균 유가(National Oil Price Summary)**:
개별 휴게소 주유 가격을 비교할 때 기준이 되는 전국 평균값. `NationalOilPriceService`가
매일 갱신한다.

## 관련 문서

- 아키텍처/설계 판단 어휘: [rules/backend/module-design.md](rules/backend/module-design.md)
- 레이어별 규칙: [rules/backend/index.md](rules/backend/index.md)
