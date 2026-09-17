# 관리자 화면 연결 계획

## 범위

- 현재 관리자 인증 브랜치에서 Java/backend Controller와 HTML template만 수정한다.
- 로그인 성공 후 `/admin`이 최소 관리자 화면을 반환하도록 연결한다.
- 메뉴·사진·휴게소 데이터·CRUD·신규 API는 추가하지 않는다.
- 기존 SecurityFilterChain과 공개 API 동작은 유지한다.

## 스펙 영향도 확인

- Endpoint: `GET /admin`이 `admin` Thymeleaf view를 반환한다.
- Request: `/admin`은 기존 `ADMIN` 인증 규칙을 그대로 사용한다.
- Response: 인증된 ADMIN에게 제목과 CSRF 포함 로그아웃 POST 폼을 반환한다.
- Error: 익명 사용자는 로그인으로 이동하고 비관리자는 403을 받는다.
- Data model: 변경 없음.

## 접근 방법 비교와 선택 이유

- 선택: 기존 `formLogin` 성공 경로와 연결되는 단일 Controller 및 정적 Thymeleaf template을 추가한다. 화면 목적을 확인하는 최소 진입점만 제공한다.
- 제외: 관리자 데이터 조회나 CRUD를 함께 추가한다. 다음 작업의 범위를 침범하고 인증 연결 검증을 어렵게 만든다.
- 선택: 로그아웃은 CSRF 토큰이 포함된 POST form으로 제공한다. Spring Security의 CSRF 보호를 끄지 않고 실제 로그아웃을 동작시킬 수 있다.

## 검증

- Controller view 반환과 실제 로그인 후 `/admin` 200 응답을 검증한다.
- 익명 접근, 비관리자 403, 로그아웃 폼과 기존 공개 API 회귀를 검증한다.
