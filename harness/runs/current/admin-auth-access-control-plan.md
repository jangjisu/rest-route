# 관리자 인증·접근 제어 계획

## 범위

- `origin/main` 최신 기준 작업 브랜치에서 Java/backend만 수정한다.
- Spring Security 기본 로그인 폼과 세션 인증을 추가한다.
- 관리자 계정 조회와 `ADMIN` 권한 검사를 추가한다.
- `/admin/**`와 `/api/admin/**`만 보호한다.
- 기존 메인 화면, 정적 리소스, 일반 조회 API는 공개 상태를 유지한다.
- 관리자 화면, CRUD, 회원가입, 비밀번호 찾기, 사진 기능은 추가하지 않는다.
- 문서와 push는 작업 범위에서 제외한다.

## 구현 단위

1. `spring-boot-starter-security`와 테스트용 Security 의존성을 추가한다.
2. `AdminUserEntity`, `AdminUserRepository`, DB 기반 `UserDetailsService`를 추가한다.
3. BCrypt `PasswordEncoder`와 `SecurityFilterChain`을 추가한다.
4. 기본 로그인 폼, `/admin/**`·`/api/admin/**` 권한 규칙, 공개 경로 규칙을 설정한다.
5. 로그인 성공 후 `/admin`으로 이동하도록 설정한다. 실제 `/admin` 화면은 다음 작업에서 추가하므로 현재는 인증 통과 후 미존재 리소스 응답이 발생할 수 있다.

## 계정 운영 정책

- 애플리케이션에 회원가입이나 계정 생성 로직을 넣지 않는다.
- 운영 계정은 BCrypt 해시를 직접 SQL로 insert할 수 있는 구조로 둔다.
- 평문 비밀번호와 실제 계정 정보는 소스·테스트 고정값·Git에 저장하지 않는다.
- 테스트는 테스트 전용 계정을 DB fixture로 생성한다.

## 접근 방법 비교와 선택 이유

대안과 선택 이유를 명시한다.

- 선택: Spring Security 기본 form login과 세션 인증. 별도 로그인 화면과 토큰 발급 계층 없이 현재 서버 렌더링 구조에 가장 적은 코드로 적용할 수 있다.
- 제외: 회원가입·JWT·OAuth. 이번 관리자 단일 계정 범위에는 불필요한 계정 생명주기와 클라이언트 토큰 관리가 추가된다.
- 선택: 관리자 계정을 DB에서 조회. 운영 SQL insert와 테스트 fixture를 동일한 인증 경로로 검증할 수 있다.
- 제외: 환경변수 계정 자동 생성. 애플리케이션 시작 시 계정 생성 책임과 운영 비밀번호 처리 범위가 늘어난다.

## 스펙 영향도 확인

- 일반 사용자 API 계약: 변경 없음.
- 관리자 인증 경로와 관리자 DB 테이블: 신규.
- Endpoint: `GET /login`은 기본 로그인 폼, `POST /login`은 기본 인증 처리, `/admin/**`와 `/api/admin/**`는 `ADMIN` 인증·인가가 필요하다.
- Request: 기본 폼은 username, password와 CSRF 토큰을 전송하며 관리자 CRUD 요청은 이번 작업에서 추가하지 않는다.
- Response: 익명 관리 경로는 로그인 흐름으로 이동하고, 권한이 없는 인증 사용자는 403을 받으며, 공개 조회 응답 JSON은 변경하지 않는다.
- Error: 로그인 실패는 기본 로그인 오류 흐름을 사용하고, 인증된 비관리자의 보호 경로 접근은 403으로 처리한다.
- Data model: `admin_user` 테이블에 username, BCrypt password hash, role을 저장한다.
- 공개 조회 API 접근 정책: 변경 없음.
- Spring Security 의존성 및 세션 기반 보안 계층: 신규.
- 문서/API 명세: 변경 없음.

## 검증

- 익명 사용자의 관리자 경로 접근은 로그인 흐름으로 이동한다.
- 인증된 비관리자 사용자는 403을 받는다.
- ADMIN 사용자는 관리자 경로의 보안 필터를 통과한다.
- 기본 로그인 성공·실패와 CSRF 보호를 검증한다.
- 기존 공개 `/api/map-config`와 휴게소 조회 경로가 익명 접근 가능한지 검증한다.
