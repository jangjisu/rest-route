# 아키텍처 방향

이 문서는 코드 작성 시 레이어별 책임과 의존 방향을 판단하는 기준이다.

## 현재 상태

- Controller, Service, Repository, Entity와 응답 DTO를 분리한다.
- 한국도로공사, 카카오 로컬 검색과 카카오 길찾기 호출은 제공자별 Client와 API 응답 객체가 담당한다.
- Entity와 DTO는 정적 팩토리 메서드로 변환 책임을 가진다.
- 스케줄러와 시작 초기화기가 API별 SyncService를 조율한다.
- 환경공단 EV API는 `EvChargerApiClient`가 페이지 단위 조회를 담당하고, `EvChargerSyncService`가 전체 페이지 조회와 자연키 upsert를 담당한다.
- 프론트엔드는 요청 모듈, 표시 포맷터와 지도 화면 제어를 Vanilla JavaScript 모듈로 분리한다.

## 합의된 목표

- Controller는 요청 검증과 응답 반환을 담당한다.
- Service는 데이터 조회와 작업 흐름을 조율한다.
- Repository는 데이터 접근만 담당한다.
- DTO 생성에 필요한 Entity들은 Service가 전달한다.
- 필드 선택, null 처리, 합산과 표현 변환은 DTO 내부에서 수행한다.
- Service에서 응답 DTO의 필드를 하나씩 조립하지 않는다.
- 별도 Mapper는 변환 규모와 중복이 실제 문제로 확인될 때만 도입한다.
- 새 계층과 추상화보다 기존 구조 안에서 해결하는 것을 우선한다.

## 주요 흐름

- 휴게소 위치·상세·영업시설·주유소 편의시설·먹거리 동기화는 외부 API 조회를 완료한 뒤 SyncService가 트랜잭션 안에서 자연키 기준으로 upsert한다(있으면 갱신, 없으면 삽입). 외부 응답에서 사라진 행은 삭제하지 않는다(삭제 대응은 별도 후속 과제).
- 주유 가격(3시간 주기) 동기화만 트랜잭션 안에서 기존 테이블을 전체 교체한다.
- EV 충전기 정보는 페이지별 조회 결과 중 성공한 C001 데이터를 모아 트랜잭션 안에서 `statId + chgerId` 기준으로 upsert한다. 중간 페이지가 실패해도 다음 페이지 조회를 계속하며, 실패한 페이지의 기존 데이터는 삭제하지 않는다. 첫 페이지 실패나 성공 데이터 부재 시에는 저장하지 않는다.
- `RestStopServiceAreaCodeBackfillService`는 저장된 휴게소·상세·EV 충전기 데이터를 기준으로 `EvChargerStationMappingCalculator`를 호출해 좌표 300m 및 이름·주소 조건을 만족하는 매핑을 갱신한다.
- 경로 조회는 매핑된 휴게소 코드 존재 여부로 `hasEvCharger`를 만들고, 상세 조회는 매핑된 충전소의 active `chgerId` 개수를 `evChargerCount`로 반환한다.
- 휴게소 상세 조회는 `basic-info`, `facilities`, `oil-info`, `foods` feature API가 각자 필요한 Entity를 조회하고 응답 DTO가 화면용 표현으로 변환한다. 프론트엔드의 상세 요청 모듈은 이 API들을 병렬로 호출하며, 기본 정보는 필수로 취급하고 시설·주유·먹거리는 독립적인 선택 영역으로 처리한다.
- 관리자는 `PUT /api/admin/rest-stops/{serviceAreaCode}/image`로 JPEG/PNG 하나를 등록·교체하고 `DELETE /api/admin/rest-stops/{serviceAreaCode}/image`로 삭제한다. 업로드 처리기는 JPEG/PNG를 WebP 상세용(긴 변 최대 1600px)과 목록용(긴 변 최대 480px)으로 변환한 뒤 두 BLOB만 저장하며 원본은 보관하지 않는다.
- 공개 `GET /api/rest-stops/{serviceAreaCode}/images/detail|list`는 저장된 WebP 바이너리를 반환한다. 휴게소가 없으면 `404`, 휴게소는 있지만 이미지가 없으면 `204 No Content`이며, 성공 응답은 데이터 기반 ETag와 `Cache-Control: public, no-cache`로 브라우저 재검증을 지원한다.
- 기존 JSON은 BLOB을 포함하지 않는다. `basic-info`의 nullable `detailImageUrl`과 경로 휴게소 항목의 nullable `listImageUrl`만 이미지가 있을 때 해당 공개 URL을 제공한다. 경로 조회는 이미지가 있는 코드만 일괄 조회해 목록별 BLOB 조회를 피한다.
- 관리자 프론트엔드는 기존 휴게소 목록으로 대상을 선택하고 이미지 조회·등록·교체·삭제 API를 독립 모듈에서 호출한다. 사용자 프론트엔드는 `detailImageUrl`과 `listImageUrl`이 있을 때만 이미지 요소를 표시하며, 값이 없으면 요소를 숨겨 기존 레이아웃을 보존한다.
- 관리자는 `GET/PUT /api/admin/rest-stops/{serviceAreaCode}/editable`로 `RestStopEntity`(휴게소명/노선번호/노선명/좌표)와 `RestStopDetailEntity`(전화번호/브랜드/노선코드/주소/편의시설/경정비·화물휴게소 여부)의 편집 대상 필드를 조회·저장한다. 저장 시 두 엔티티 모두 `adminOverridden` 플래그가 켜지고, `RestStopSyncService`/`RestStopDetailSyncService`는 이 플래그가 켜진 행을 자동 동기화에서 건너뛴다(식별자/조인 키는 갱신하지 않아 관리자 편집이 유지됨). `RestStopDetailEntity`가 아직 없는 `RestStopEntity`(위치정보·편의시설 API 동기화 시점 차이로 발생)는 조회 시 상세 필드를 `null`로 반환하고, 저장 시 `RestStopDetailEntity.createEmpty(serviceAreaCode)`로 새로 만들어 저장한다. `DELETE .../editable/override`로 잠금을 해제하면 다음 동기화부터 다시 자동 갱신 대상이 된다.
- 관리자 페이지는 대시보드(`/admin`)·휴게소 이미지 관리(`/admin/rest-stops/images`)·휴게소 정보 관리(`/admin/rest-stops/edit`)·휴게소 음식 관리(`/admin/rest-stops/foods`)·휴게소 주유소 연결 관리(`/admin/rest-stops/oil-links`) 5개의 독립된 Thymeleaf 템플릿/라우트로 나뉜다. nav·헤더·로딩 오버레이·토스트처럼 여러 페이지가 공유하는 조각은 `templates/fragments/admin-shell.html`의 `th:fragment`(`sidebar`/`header`/`chrome`)로 분리하고 각 페이지가 `th:replace`로 삽입한다. 공용 토스트·로딩 오버레이 헬퍼(`showToast`/`setGlobalLoading`)는 `admin-common.js`로 분리해 대시보드·이미지·정보관리·음식관리·주유소연결관리 JS가 공통으로 import한다.
- 관리자 활동 로그는 AOP나 인터셉터 같은 자동 계측 대신, 관리자 쓰기 지점 각각(판매순위 업로드 2종, 전체 휴게소명 매핑, 휴게소 이미지 등록·삭제, 휴게소 정보 수정, 동기화 잠금 해제, 음식 메뉴 추가·수정·삭제·잠금 해제, 음식 메뉴 이미지 등록·삭제, 주유소 연결·연결해제·잠금해제)에서 `AdminActivityLogService`의 액션별 메서드(`logProductSalesUpload` 등)를 명시적으로 호출해 기록한다. 각 메서드는 `Authentication.getName()`으로 actor를 얻고 클래스 내부 문구 상수를 `String.format`으로 조합해 `AdminActivityLogEntity`(actor/message/createdAt)로 저장한다 — 자동 계측은 URL·HTTP 메서드 정도만 알 수 있어 휴게소명 같은 도메인 데이터가 담긴 문장을 만들 수 없기 때문에 채택하지 않았다(문구 가독성 우선, 사용자 확인). 조회는 별도 API 없이 `AdminDashboardService.getSummary()`가 최근 50건을 `AdminDashboardSummary.recentActivityLogs`에 포함시켜 반환한다.
- 관리자는 `GET/POST/PUT/DELETE /api/admin/rest-stops/{serviceAreaCode}/foods[/{foodId}[/override|/image]]`로 `RestFoodEntity`(먹거리 메뉴)를 조회·추가·수정·삭제하고 동기화 잠금을 해제한다. 별도 테이블을 새로 만들지 않고 기존 `rest_food`를 그대로 재사용하며, `rest_stop`/`rest_stop_detail`과 동일한 `adminOverridden` 패턴으로 `RestFoodSyncService.upsertOne`이 이 플래그가 켜진 행을 자동 동기화에서 건너뛴다. 관리자가 새로 추가하는 메뉴는 외부 API `seq`와 겹치지 않도록 `"ADMIN-" + UUID`로 발급하고(`RestFoodEntity.createByAdmin`) 생성 시점부터 `adminOverridden=true`로 시작한다. 동기화는 사라진 행을 삭제하지 않는 원칙이라 동기화 메뉴 자체의 삭제는 지원하지 않고(삭제해도 다음 동기화에 다시 생성됨) 관리자가 직접 추가한 메뉴만 삭제할 수 있다(`RestFoodEntity.isAdminCreated`로 `seq` 접두사 판별, 아니면 `AdminRestFoodService.delete`가 `400 Bad Request` 예외를 던짐). 메뉴 이미지는 `RestStopImageProcessor`(WebP 상세/목록 변환)를 엔티티에 의존하지 않는 순수 변환기로 그대로 재사용하되, 휴게소 1건당 이미지 0~1개인 `rest_stop_image`와 달리 메뉴 1건당 이미지 0~1개인 `rest_food_image`(PK=`foodId`)에 저장한다 — 사용자 화면에 노출할 공개 조회 API는 표시 방식이 정해지지 않아 이번 범위에서 만들지 않는다. 다만 관리자가 방금 등록한 이미지를 스스로 확인할 수 있도록 `AdminRestFoodImageQueryService`/`GET /api/admin/rest-stops/{serviceAreaCode}/foods/{foodId}/image`(관리자 전용 목록용 WebP 미리보기, 공개 API 아님)를 추가했다. 이 GET과 기존 PUT/DELETE 이미지 엔드포인트는 `AdminRestFoodController`(JSON `ApiResponse` CRUD 전담)가 아니라 별도의 `AdminRestFoodImageController`(`@Controller`+`@ResponseBody`)에 둔다 — `RestStopImageController`/`AdminRestStopImageController`가 이미지 바이너리·Void 응답을 JSON API 컨트롤러와 분리하는 것과 동일한 패턴이다.
- `rest_stop`↔`rest_oil` 자동 매칭(`route_no = route_code` + 정규화 이름 일치)과, 그 결과를 한 단계 더 거쳐 파생되는 `rest_stop`↔`rest_oil_price` 매칭(`rest_oil.standard_rest_code = rest_oil_price.service_area_code2`)은 `RestStopServiceAreaCodeBackfillService.backfillRestOils()`/`backfillRestOilPrices()`가 매일 자정·3시간마다·서버 시작 시(`RestStopScheduler`의 두 스케줄과 `RestStopStartupInitializer`) 다시 계산해 덮어쓰므로, fuzzy 매칭이 놓치거나 잘못 연결한 값을 관리자가 고쳐도 다음 실행에 되돌아갈 수 있었다. `RestOilEntity`와 `RestOilPriceEntity` 양쪽에 `rest_stop`/`rest_food`와 동일한 `adminOverridden` 플래그를 추가하고 두 backfill 메서드가 각각 이 플래그가 켜진 행을 건너뛰도록 가드를 추가했다. 관리자 화면의 "주유소" 단위는 `rest_oil_price`다 — `rest_oil`은 물리적 주유소 하나당 부대 편의시설(세차장·쉼터 등) 개수만큼 여러 행이 생기는 반면(예: 서울만남(부산)주유소는 편의시설 2행), 실제 사용자에게 보이는 업체·가격 정보가 담긴 `rest_oil_price`는 휴게소당 정확히 0~1건이라 "휴게소당 주유소 1개"라는 사용자의 실제 멘탈 모델과 일치하기 때문이다. 관리자는 `GET /api/admin/rest-stops/oil-links`로 전체 휴게소와 연결된 주유소(0~1건, 주유소명·노선명·주소·방향 포함)를 한 번에 조회하고, `GET /api/admin/oil-stations/search`로 이름 검색 후 `PUT .../{oilId}/link`(연결)·`DELETE .../{oilId}/link`(해제, 둘 다 `adminOverridden=true`로 잠금)·`DELETE .../{oilId}/override`(잠금 해제, 다음 배치부터 재계산)로 조정한다. `AdminRestOilLinkService`는 이 세 조작을 대상 `rest_oil_price` 행뿐 아니라 같은 자연키(`standardRestCode`/`serviceAreaCode2`)를 공유하는 `rest_oil` 행 전체에도 함께 적용해(cascade) 두 테이블의 연결 상태가 갈라지지 않게 한다. "관리자가 확인했지만 실제로 연결 없음"이라는 상태는 별도로 추적하지 않는다(`adminOverridden`은 개별 행에만 있어 완전히 연결 해제된 휴게소 쪽에는 남길 방법이 마땅치 않고, 1인 운영 환경에서 과설계로 판단).
- 전국 평균 유가 요약은 `/api/national-oil-prices/summary`가 별도로 조회·반환한다. 경로 Service는 이 요약을 휴게소별 `comparisonSummary` 계산에 사용할 수 있지만, `RouteRestStopResponse`의 최상위 응답에는 포함하지 않는다.
- 경로 탐색은 카카오 장소 검색 후보에서 선택한 좌표로 길찾기를 호출하고, 저장된 휴게소 좌표와 경로 사이 거리를 계산한다.
- 휴게소 이름 검색은 `GET /api/rest-stops/search?name=`이 `RestStopRepository.findByUnitNameContainingIgnoreCase`로 DB를 직접 조회해 `serviceAreaCode`를 포함한 `RestStopItemResponse` 목록을 반환한다. 카카오 지오코딩 기반 `/api/place-search`(출발지·도착지 검색용, `serviceAreaCode` 없음)와는 별도 경로다. 프론트엔드는 결과가 1건이면 바로, 여러 건이면 목적지 검색과 동일한 스타일의 후보 모달(`restStopSearchModal`, `placeCandidateModal`과 별도 인스턴스)에서 고른 뒤 기존 마커 클릭과 동일한 `openDetailPanel`로 상세 정보를 연다 — 경로 탐색 사이드이펙트(엔드포인트 마커 생성, 경로 재요청)는 발생시키지 않는다.
- 카카오 API 예외는 `GlobalExceptionHandler`가 공통 응답으로 변환하고, 정기 동기화 예외는 스케줄러가 항목별로 기록한다.
- EV API 요청은 페이지 번호와 건수 중심으로 로그를 남기며 API key와 응답 payload는 로그에 남기지 않는다. EV API 전용 read timeout은 60초이고 다른 Feign client의 기본 timeout은 유지한다.
- 이 기능은 내부 관리자·공개 조회 계약만 추가하며, 한국도로공사·카카오·오피넷·환경공단 외부 API의 요청·응답과 호출 정책은 변경하지 않는다.

## 제어 흐름 원칙

- Java 코드에서 `else`와 `else if`를 사용하지 않는다.
- 실패 조건은 guard clause, early return 또는 예외로 먼저 종료한다.
- 여러 조건 선택이 필요하면 `switch`, 다형성 또는 명시적인 분리 메서드를 검토한다.
- 대안이 더 복잡해지는 특별한 경우에는 구현 전에 사용자와 이유를 합의한다.

## 미결정 사항

- 조회 조합이 커질 때 도입할 전용 Query 계층 또는 Mapper의 기준
- 외부 API 장애 시 캐시와 재시도 정책
