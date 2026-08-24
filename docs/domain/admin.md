---
domain: admin
aliases: ["관리자", "admin"]
paths:
  - "src/main/java/com/restroute/domain/AdminUserEntity.java"
  - "src/main/java/com/restroute/domain/AdminActivityLogEntity.java"
  - "src/main/java/com/restroute/domain/AdminRole.java"
  - "src/main/java/com/restroute/service/admin/**"
  - "src/main/java/com/restroute/service/admindashboard/**"
  - "src/main/java/com/restroute/controller/admin/**"
  - "src/main/java/com/restroute/config/SecurityConfig.java"
  - "src/main/resources/templates/admin-*.html"
  - "src/main/resources/templates/fragments/admin-shell.html"
  - "src/main/resources/static/js/admin-*.js"
related_domains: ["rest-stop", "oil-price", "ev-charger", "rest-stop-content"]
sources:
  - "c00c767 feat: add admin authentication and access control"
  - "ae4a944 feat: 관리자 휴게소 정보 편집 백엔드 API 추가"
  - "698d805 feat: 관리자 대시보드에 활동 로그(최근 작업) 기능 추가"
  - "c326370 feat: 관리자가 휴게소 먹거리 메뉴를 직접 추가·수정할 수 있는 기능 추가"
  - "cef3d4c feat: 관리자가 휴게소-주유소 연결을 점검·수정할 수 있는 기능 추가"
  - "72af422 feat: 항공권 공휴일 관리자 페이지 추가"
  - "1063d48 feat: 관리자 휴게소 신규 등록 기능 추가"
---

# admin

## 1. 목적과 범위

이 앱은 공공데이터(오피넷, 도로공사 등)로 휴게소·주유소·먹거리 정보를 자동 동기화하는데, 자동 동기화 데이터에는 오류·누락이 있을 수 있다. `admin` 도메인은 운영자가 이 자동 동기화 결과를 검수하고 수동으로 교정할 수 있게 하는 백오피스다. 범위는 (1) 관리자 인증/인가, (2) 휴게소 기본정보·이미지·먹거리 메뉴·주유소 연결의 수동 편집(override), (3) 판매순위 CSV 업로드, (4) 항공권 공휴일 CRUD, (5) 대시보드 요약과 최근 작업 로그다. 실제 도메인 데이터(휴게소, 주유소, 먹거리)의 소유권은 각 도메인에 있고, admin은 그 데이터에 대한 "쓰기 게이트웨이" 역할만 한다 — 별도의 admin 전용 비즈니스 데이터는 admin 계정, 활동 로그, 공휴일 정도다.

## 2. 용어와 핵심 엔티티

- **AdminUserEntity** (`admin_user` 테이블): `username`(unique), `password`(BCrypt 해시), `role`(`AdminRole`). Spring Security `UserDetailsService`(`AdminUserDetailsService`)가 로그인 시 조회한다.
- **AdminRole**: enum이지만 현재 `ADMIN` 값 하나뿐이다 — 역할 세분화(예: 읽기 전용 관리자)는 아직 없다.
- **AdminActivityLogEntity** (`admin_activity_log` 테이블): `actor`(`Authentication.getName()`, 즉 로그인한 관리자 아이디), `message`(사람이 읽는 로그 문구), `createdAt`(주입된 `Clock` 기준 — 테스트 용이성을 위해 `LocalDateTime.now()` 대신 `Clock`을 사용). `AdminActivityLogService`는 쓰기 지점마다 전용 로그 메서드(`logProductSalesUpload`, `logRestStopEdited`, `logRestStopOverrideCleared`, `logCustomFoodAdded`, `logOilStationLinked`, `logFlightHolidayAdded` 등 19개)를 두고, 각 메서드는 고정된 문구 템플릿(예: `"%s의 동기화 잠금을 해제했습니다."`, `"%s 주유소를 %s에 연결했습니다."`)에 값을 채워 저장한다. 대시보드 "최근 작업" 패널에 사용된다.
- **동기화 잠금 / override (adminOverridden)**: `RestStopEntity`, `RestStopDetailEntity`, `RestFoodEntity` 등에 있는 boolean 플래그. 관리자가 값을 직접 수정하면 `true`로 세팅되어 이후 배치 동기화가 해당 행을 건너뛴다. "override 해제"는 이 플래그를 다시 `false`로 돌려 자동 동기화 대상으로 복귀시키는 것이다. admin 도메인이 다른 도메인 데이터를 다룰 때 가장 핵심적인 계약이다.
- **AdminDashboardSummary**: 휴게소 총 개수, 최신 판매순위 반영월, EV 충전 준비 상태(현재 문자열 `"준비중"` 하드코딩 — 추정: EV 충전 도메인 대시보드 연동 전 placeholder, 확인 필요), 최근 활동 로그 목록을 담는 DTO.
- **FlightHolidayEntity**: 날짜(unique)+이름을 저장하는 공휴일 테이블. 항공권 검색 결과의 공휴일 필드에 반영하기 위한 것이지만, 실제 항공권 검색과의 연동은 이 기능이 추가된 시점(72af422)에는 아직 없었다 — "flight 검색 응답의 holiday 필드... 연동은 별도 작업" (커밋 메시지 원문). 연동 완료 여부는 확인 필요.

## 3. 사용자·시스템 흐름

1. 관리자가 `/login`에서 폼 로그인 → 성공 시 `/admin`(대시보드)로 리다이렉트(`defaultSuccessUrl`).
2. 대시보드(`GET /api/admin/dashboard`)에서 휴게소 수/판매순위 반영월/최근 활동 로그 확인, 필요 시 판매순위 CSV 업로드(EUC-KR/MS949) 또는 서비스 지역 코드 백필 실행.
3. 좌측 메뉴(admin-shell)에서 휴게소 정보 편집·이미지 관리·먹거리 메뉴 관리·주유소 연결 관리·공휴일 관리 중 하나로 이동 — 각 화면은 서버사이드 렌더링된 Thymeleaf 템플릿(`AdminController`가 GET으로 뷰 이름만 반환) + 화면별 JS가 REST API를 호출하는 구조.
4. 각 편집 동작(정보 수정/메뉴 추가·수정·삭제/이미지 등록·삭제/주유소 연결·해제/공휴일 추가·삭제/판매순위 업로드/백필)은 완료 후 `AdminActivityLogService`의 전용 로그 메서드를 호출해 활동 로그 한 줄을 남긴다.
5. "override 해제"(동기화 잠금 해제) 시점부터 해당 행은 다음 배치 동기화에서 다시 자동 갱신 대상이 된다.

## 4. 정책과 불변 조건

- `/admin/**`, `/api/admin/**`는 `ROLE_ADMIN`만 접근 가능(`SecurityConfig`). 그 외 `/`, `/login`, 정적 리소스, `/api/**`(공개 조회 API)는 인증 없이 접근 가능 — 즉 일반 조회 API는 비로그인 사용자에게도 열려 있고, admin API만 별도로 잠긴 구조다.
- 관리자가 값을 직접 수정하면 해당 레코드는 `adminOverridden=true`가 되어 자동 동기화에서 **영구 제외**된다(관리자가 명시적으로 override를 해제하기 전까지). 이 규칙은 `RestStopSyncService`/`RestStopDetailSyncService` 등 동기화 쪽 코드가 지켜야 하는 불변 조건이다.
- 활동 로그는 AOP/인터셉터 방식이 아니라 각 서비스 메서드에서 명시적으로 로그 메서드를 호출하는 방식으로 구현되어 있다 — "사용자와 논의 후 채택, 로그 문구 가독성 우선"(698d805 커밋 메시지). 새 관리 기능을 추가할 때도 이 패턴(각 쓰기 지점에서 명시적 로그 호출)을 따라야 한다는 것이 기존 관례로 보인다.
- 좌표값 등 입력은 서비스 레이어에서 검증하고 실패 시 `InvalidRestStopEditException`(400) 등 도메인 예외로 매핑한다(`service/admin/exception/` 패키지).
- 공휴일 동기화는 주말도 저장 대상에 포함하도록 정책이 바뀐 이력이 있다(8c70b65) — 최초엔 주말 제외였다가 이후 변경됨. 현재 코드 기준 정책만 반영했고, 변경 이유의 세부 배경은 커밋 메시지만으로는 완전히 확인되지 않음(추정 — 확인 필요).

## 5. 상태와 데이터 수명주기

- **AdminUserEntity**: 생성 경로가 코드베이스 내(마이그레이션, seed, CommandLineRunner)에서 발견되지 않음 — 관리자 계정이 어떻게 최초 생성되는지(수동 DB insert 등) **추정 — 확인 필요**.
- **AdminActivityLogEntity**: 쓰기 전용에 가까움 — 생성만 되고 수정/삭제 API는 없음. 조회는 `findTop50ByOrderByCreatedAtDesc()` 하나뿐이며(리포지토리 메서드명에 상한 50건이 고정됨), 대시보드 요약(`getSummary()`) 응답 안에 `recentActivityLogs`로 포함되는 형태다 — 별도의 전체 목록/페이지네이션 API는 없다.
- **override 대상 엔티티(RestStopEntity 등)**: override 설정(admin 수정) → override 해제(admin이 명시적으로 잠금 해제) 순환. override 상태인 동안은 동기화 배치가 그 행을 skip한다.
- **FlightHolidayEntity**: `POST`로 생성(날짜 중복 방지), `DELETE`로 id 기준 삭제. 별도 만료/자동 정리 로직은 확인되지 않음.
- 이미지(휴게소/먹거리 메뉴): `PUT`으로 업로드·교체, `DELETE`로 삭제. 저장 방식(파일시스템/DB blob/외부 스토리지)은 `RestStopImageProcessor` 등에 위임되어 있어 이 문서에서는 상세 확인 안 함(추정 — 확인 필요, rest-stop-content 도메인 문서 참조 권장).

## 6. UI·오류·권한 상태

- 미인증 상태로 `/admin/**` 또는 `/api/admin/**` 접근 시 Spring Security가 로그인 폼으로 리다이렉트(또는 401/403 — 정확한 응답 형태는 `SecurityConfigTest` 확인 권장).
- 대시보드 "최근 작업" 패널은 최근 5개만 먼저 보여주고 "전체 보기" 클릭 시 모달로 전체(최대 50건) 표시 — 과거에는 alert였다가 모달로 UX 개선(698d805).
- 먹거리 관리 화면에서 수정 버튼 라벨, 이미지 삭제 버튼 노출 조건, 이미지 미리보기 문제 등이 별도 버그 수정 커밋(4c09c3f)으로 다뤄진 이력이 있음 — 현재는 수정된 상태로 가정.
- 관리자 화면은 전부 서버 렌더링 Thymeleaf + 공통 JS(`admin-common.js`)와 화면별 request/page JS 조합. 로딩/에러 상태 처리는 화면별 JS 파일 내부 로직에 있으며 공통 표준 컴포넌트로 추상화되어 있는지는 확인 안 함(추정 — 확인 필요).

## 7. 외부 시스템과 계약

- admin 도메인 자체는 외부 API를 직접 호출하지 않는다. 대신 판매순위 CSV(EUC-KR/MS949 인코딩, 헤더 포함, 예: `product_sales_2026-07.csv`)를 관리자가 업로드하면 `SalesRankingUploadService`가 파싱해 저장한다.
- `RestStopServiceAreaCodeBackfillService.backfill()`을 통해 서비스 지역 코드 매핑을 일괄 재계산하는 관리자 전용 배치 트리거가 있다.
- 그 외 실제 외부 연동(카카오/오피넷 등)은 각 데이터 소유 도메인(rest-stop, oil-price 등)의 동기화 서비스가 담당하고, admin은 그 결과를 override할 뿐이다.

## 8. 코드 경계와 진입점

- **엔티티**: `domain/AdminUserEntity.java`, `domain/AdminActivityLogEntity.java`, `domain/AdminRole.java`.
- **서비스**: `service/admin/*Service.java`(휴게소 편집, 먹거리 메뉴/이미지, 주유소 연결, 공휴일, 활동 로그, 사용자 조회) + `service/admin/exception/*`(도메인 예외) + `service/admindashboard/AdminDashboardService.java`, `service/admindashboard/dto/*`.
- **컨트롤러**: `controller/admin/AdminController`(뷰 라우팅: `/admin`, `/admin/rest-stops/{images,edit,foods,oil-links}`, `/admin/flights/holidays`), `AdminDashboardController`(`/api/admin/dashboard`, 판매순위 업로드/백필), `AdminRestStopEditController`(`/api/admin/rest-stops/{code}/editable`, override 해제), `AdminRestFoodController`(`/api/admin/rest-stops/{code}/foods`), `AdminRestFoodImageController`/`AdminRestStopImageController`(이미지 CRUD), `AdminRestOilLinkController`(`/api/admin/oil-stations/**`, `/api/admin/rest-stops/oil-links`), `AdminFlightHolidayController`(`/api/admin/flights/holidays`).
- **보안**: `config/SecurityConfig.java` — `/admin/**`, `/api/admin/**` → `ROLE_ADMIN`, formLogin/logout 설정.
- **프론트엔드**: `templates/admin-dashboard.html`, `admin-rest-stop-edit.html`, `admin-rest-stop-images.html`, `admin-rest-stop-foods.html`, `admin-rest-stop-oil-links.html`, `admin-flight-holidays.html`, 공통 레이아웃 `templates/fragments/admin-shell.html`. JS는 `static/js/admin-*.js`(화면별 page/request 페어 + `admin-common.js`). 참고: 태스크 브리프에 언급된 `admin.html` 단일 파일은 존재하지 않고, 화면별로 분리된 템플릿 + 공통 shell fragment 구조다.
