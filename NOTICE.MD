# NOTICE: Spring Web 개발 시 구현해야 할 핵심 항목

이 문서는 `db/init/schema.sql` 기준으로, Spring Web 프로젝트에서 구현해야 하는 기능을 체계적으로 정리한 개발 체크리스트입니다.

## 1. 인증/회원 기능

### 1.1 회원가입/로그인
- [x] 로컬 회원가입(아이디/비밀번호 기반)
- [x] 로컬 로그인 (JWT Access + Refresh Token 발급)
- [x] 로그아웃 (Redis 토큰 폐기)
- [x] 소셜 회원가입(OAuth2: Google/Kakao/Naver)
- [x] 로컬/소셜 계정 병합 정책(동일 이메일 시 로컬 ID/PW 확인 후 연동)

### 1.1-1 2차 인증(2FA)
- [ ] 이메일/휴대폰 2차 인증 지원
- [ ] OTP 또는 코드 기반 인증 플로우 제공
- [ ] 2차 인증 코드 발급/만료/재전송 정책 정의

### 1.2 JWT 토큰 발급
- [x] Access Token (30분) / Refresh Token (7일) 발급
- [x] JWT Claim 설계 (sub=userPk, uid=userId, sid=sessionId)
- [x] Refresh Token Rotation 정책 적용
- [x] admin-api / user-api JWT 인증 분리 (별도 secret key + issuer 클레임으로 토큰 교차 사용 방지)

### 1.3 토큰 저장/검증
- [x] Redis에 Access/Refresh 토큰 저장 (auth:access:{userPk}:{sessionId})
- [x] 토큰 폐기/강제 로그아웃 처리
- [ ] 중복 접속 차단 정책 적용(단일 세션 또는 N개 허용)

### 1.4 사용자 상태 관리
- [ ] 이메일/휴대폰 인증 처리 (`email_verified`, `phone_verified`)
- [x] 잠금 정책 (`login_fail_count` 5회 초과 시 `is_locked` = true)
- [x] 계정 상태 관리 (`user_status`: PENDING → ACTIVE → SUSPENDED)
- [ ] 회원가입 시 자동 인증(ACTIVE) On/Off 설정 (시스템 설정 연동)

### 1.5 페이지/리소스별 2FA 정책
- [ ] 특정 페이지/기능 접근 시 2FA 요구 정책 정의
- [ ] 2FA 필요 여부 판단 로직(권한 체크 이전/이후) 결정
- [ ] 2FA 성공 시 일정 기간 재요구 여부(세션 단위/시간 단위) 결정

## 2. 감사 로깅 (필수)

### 2.1 감사 로그 기록
- [ ] 로그인/로그아웃/토큰 발급/폐기 기록 (`tb_audit_logs`)
- [ ] 권한 변경/모듈 생성/그룹 가입 등 주요 행위 기록
- [ ] 실패 이벤트도 기록 (예: 로그인 실패)

### 2.2 감사 로그 필드 기준
- [ ] 행위자 (actor_user_id, actor_type)
- [ ] 대상 정보 (target_type, target_id)
- [ ] 결과 (result_status)
- [ ] IP / User-Agent / 메타데이터

## 3. 그룹/조직 관리

### 3.1 그룹 생성 및 소속
- [x] 그룹 생성/삭제 (admin-api: POST/DELETE /groups)
- [x] 그룹 수정 (admin-api: PUT /groups/{id})
- [x] 그룹 목록/상세 조회 (admin-api: GET /groups, GET /groups/{id})
- [x] 시스템 그룹 보호 (administrator, user — 삭제/코드변경 불가)
- [x] 회원가입 시 기본 그룹(user) 자동 배정
- [x] 그룹 멤버 추가/제거 (admin-api: POST/DELETE /groups/{id}/members)
- [x] 그룹 멤버 목록 조회 (admin-api: GET /groups/{id}/members)
- [x] 내 소속 그룹 조회 (user-api: GET /groups/me)
- [ ] 그룹 소유자 변경/관리자 정책
- [ ] 그룹 멤버 초대(`tb_group_invites`) 및 가입 처리

### 3.2 그룹 역할
- [ ] 그룹 내 역할 부여 (`tb_group_member_roles`)
- [ ] 그룹 단위 권한 부여 (`tb_group_roles`)

## 4. 모듈/인스턴스 관리

### 4.1 모듈 관리
- [ ] 모듈 정의 등록 (`tb_modules`)
- [ ] 모듈 권한 등록 (`tb_module_permissions`)
- [ ] ModuleDefinition 인터페이스 구현
- [ ] ModuleRegistry (모듈 자동 발견/등록)
- [ ] SlugResolver (slug → 모듈/인스턴스 매핑)

### 4.2 모듈 인스턴스 관리
- [ ] 인스턴스 생성/삭제 (`tb_module_instances`)
- [ ] 소유자 관리 (`owner_id`, `instance_type`)
- [ ] 인스턴스 활성/비활성 처리 (`enabled`)
- [ ] Slug 기반 동적 라우팅 (GET /resolve/{module-slug}/{instance-slug})

## 5. 권한 체크 로직

### 5.1 권한 체크 흐름(권장)
1. [ ] 전역 역할(`tb_user_roles`) 확인
2. [ ] 모듈 인스턴스 소유자 여부 확인
3. [ ] 사용자 직접 권한(`tb_user_module_permissions`) 확인
4. [ ] 그룹 권한(`tb_group_module_permissions`) 확인

### 5.2 권한 체크 구현 위치
- [ ] PermissionChecker 유틸리티 (플랫 문자열 기반: BOARD_POST_WRITE)
- [ ] @RequirePermission 어노테이션

## 6. 메뉴 관리

### 6.1 메뉴 CRUD (admin-api)
- [ ] 메뉴 항목 생성/수정/삭제 (POST/PUT/DELETE /menus)
- [ ] 메뉴 트리 전체 조회 (GET /menus)
- [ ] 메뉴 정렬 순서 변경 (PATCH /menus/{id}/order)
- [ ] 메뉴 유형: MODULE / LINK / SEPARATOR

### 6.2 메뉴 조회 (user-api)
- [ ] 권한 기반 필터링된 메뉴 트리 반환 (GET /menus/me)
- [ ] MODULE 타입: 연결된 인스턴스에 *_*_READ 권한 있으면 표시
- [ ] SEPARATOR 타입: 하위 메뉴 중 하나라도 보이면 표시

## 7. 시스템 설정

### 7.1 기능 플래그
- [ ] 회원가입 활성화 여부 (`tb_system_settings`, 비활성 시 가입 API 차단)
- [ ] 회원가입 시 자동 인증 On/Off (`tb_system_settings`, On이면 가입 즉시 ACTIVE, Off면 PENDING)
- [ ] 소셜 로그인 사용 여부 (`tb_system_settings`)
- [ ] 2차 인증 필수 여부 및 적용 범위 (`tb_system_settings`)

### 7.2 관리자 페이지 접근 제어
- [ ] admin-api 접근 시 administrator 그룹 소속 여부 검증 (JWT 필터 또는 SecurityConfig에서 차단)
- [ ] 관리자 페이지 접근 가능한 사용자/그룹 관리 (시스템 설정 또는 별도 테이블)
- [ ] 비관리자가 admin-api 로그인 시도 시 적절한 에러 응답 (AUTH_ACCESS_DENIED)

### 7.3 설정 적용 방식
- [ ] 애플리케이션 시작 시 캐시 로딩
- [ ] 관리 API를 통한 실시간 수정
- [ ] 설정 CRUD API (admin-api: GET/PUT /settings)

### 7.3 시스템 초기 설정 (Setup Wizard)
- [x] 초기화 상태 확인 (administrator 그룹 멤버 유무로 판단)
- [x] 최초 관리자 계정 생성 API (POST /setup/initialize)
- [x] SetupGuardFilter (미초기화 시 /setup/** 외 요청 차단)
- [x] 관리자 웹 초기 설정 위자드 페이지

## 8. API 설계 (구현 현황)

### 8.1 인증/회원 (user-api)
- [x] POST `/auth/signup` — 회원가입
- [x] POST `/auth/login` — 로그인
- [x] POST `/auth/refresh` — 토큰 갱신
- [x] POST `/auth/logout` — 로그아웃
- [x] GET `/auth/me` — 현재 사용자 정보 (grace period 5초)
- [x] GET `/auth/check/user-id` — 아이디 중복 확인
- [x] GET `/auth/check/email` — 이메일 중복 확인
- [x] GET `/auth/oauth2/providers` — 활성 소셜 Provider 목록
- [x] GET `/auth/oauth2/authorize/{provider}` — Authorization URL 생성
- [x] GET `/auth/oauth2/callback/{provider}` — OAuth2 콜백 처리
- [x] POST `/auth/oauth2/link-confirm` — 소셜 연동 확인 (로컬 ID/PW 검증)
- [x] GET `/auth/oauth2/identities` — 내 소셜 연동 목록
- [x] GET `/auth/oauth2/link/{provider}` — 마이페이지 연동용 Authorization URL
- [x] GET `/auth/oauth2/link-callback/{provider}` — 마이페이지 연동 콜백
- [x] DELETE `/auth/oauth2/identities/{id}` — 소셜 연동 해제

### 8.1-1 인증/회원 (admin-api)
- [x] POST `/auth/login` — 관리자 로그인
- [x] POST `/auth/logout` — 관리자 로그아웃
- [x] POST `/auth/refresh` — 관리자 토큰 갱신
- [x] GET `/auth/me` — 관리자 현재 사용자 정보
- [x] GET `/auth/providers` — 소셜 Provider 전체 목록
- [x] PUT `/auth/providers/{id}` — Provider 설정 수정

### 8.1-2 회원 관리 (admin-api)
- [x] GET `/users` — 회원 목록 (페이지네이션)
- [x] GET `/users/{id}` — 회원 상세 조회
- [x] PUT `/users/{id}` — 회원 정보 수정 (이름/이메일/상태)
- [x] PUT `/users/{id}/password` — 회원 비밀번호 변경
- [x] DELETE `/users/{id}` — 회원 삭제 (자기 자신/관리자 그룹 소속 보호, Redis 토큰 정리)
- [x] PUT `/users/{id}/unlock` — 회원 잠금 해제
- [x] GET `/users/{id}/identities` — 회원 소셜 연동 목록

### 8.1-3 시스템 초기 설정 (admin-api)
- [x] GET `/setup/status` — 시스템 초기화 상태 확인
- [x] POST `/setup/initialize` — 최초 관리자 계정 생성

### 8.2 그룹 (admin-api)
- [x] POST `/groups` — 그룹 생성
- [x] GET `/groups` — 전체 그룹 목록
- [x] GET `/groups/{id}` — 그룹 상세 조회
- [x] PUT `/groups/{id}` — 그룹 수정
- [x] DELETE `/groups/{id}` — 그룹 삭제
- [x] GET `/groups/{id}/members` — 그룹 멤버 목록
- [x] POST `/groups/{id}/members` — 멤버 추가
- [x] DELETE `/groups/{id}/members/{userId}` — 멤버 제거

### 8.3 그룹 (user-api)
- [x] GET `/groups/me` — 내 소속 그룹 목록

### 8.4 모듈 (미구현)
- [ ] POST `/modules`
- [ ] GET `/modules`
- [ ] POST `/modules/{code}/instances`
- [ ] GET `/modules/{code}/instances/{instanceId}`
- [ ] GET `/resolve/{module-slug}/{instance-slug}`

### 8.5 권한 (미구현)
- [ ] POST `/permissions/grant`
- [ ] POST `/permissions/revoke`
- [ ] GET `/permissions/check`

### 8.6 메뉴 (미구현)
- [ ] POST `/menus` — 메뉴 생성 (admin-api)
- [ ] GET `/menus` — 메뉴 트리 조회 (admin-api)
- [ ] PUT `/menus/{id}` — 메뉴 수정 (admin-api)
- [ ] DELETE `/menus/{id}` — 메뉴 삭제 (admin-api)
- [ ] GET `/menus/me` — 권한 기반 메뉴 트리 (user-api)

## 9. 프론트엔드

### 9.1 사용자 웹 (frontend/user)
- [x] 회원가입 페이지
- [x] 로그인 페이지 (소셜 로그인 버튼 포함)
- [x] 프로필 페이지 (소셜 계정 연동/해제)
- [x] 소개 페이지
- [x] 소셜 로그인 콜백 처리 페이지
- [x] 소셜 연동 확인 페이지 (기존 계정 발견 시 로컬 ID/PW 입력)
- [x] 소셜 연동 성공 콜백 페이지
- [x] shadcn/ui 컴포넌트 전환
- [x] toast 알림 (sonner) 적용
- [ ] 동적 메뉴 렌더링

### 9.2 관리자 웹 (frontend/admin)
- [x] 관리자 로그인 페이지
- [x] 초기 설정 위자드 페이지
- [x] 대시보드 페이지
- [x] 회원 목록 페이지 (인라인 상태 변경, 삭제)
- [x] 회원 상세 페이지 (정보 수정, 비밀번호 변경, 잠금 해제, 소셜 연동 표시)
- [x] 그룹 관리 페이지 (목록/상세/멤버 관리)
- [x] 소셜 Provider 설정 관리 페이지 (settings/auth-providers)
- [x] shadcn/ui 컴포넌트 전환
- [x] JWT 인증 컨텍스트 및 API 유틸리티
- [ ] 모듈/인스턴스 관리 페이지
- [ ] 메뉴 관리 페이지
- [ ] 시스템 설정 페이지
- [ ] 감사 로그 조회 페이지

## 10. 기타 고려사항

- [x] 멀티 프로젝트 구조 설계 (core / admin-api / user-api)
- [x] 공통 예외 처리 (GlobalExceptionHandler + ErrorCode 인터페이스)
- [x] 공통 응답 래퍼 (ApiResponseDto)
- [x] Spring Security + JWT 필터 체인
- [x] Redis 토큰 세션 관리
- [x] application.properties → application.yml 형식 전환
- [x] 필터 체인 에러 응답 ApiResponseDto 직렬화 통일 (FilterErrorResponseWriter)
- [x] 페이지네이션 공통 응답 DTO (PageResponseDto)
- [ ] **admin-api 접근 제어: administrator 그룹 소속자만 허용 (보안 필수)**
- [ ] 트랜잭션 처리 범위 정의
- [ ] FK 제약 위반 대비 예외 처리
- [ ] DB 마이그레이션 도입(Flyway 권장)
- [ ] 보안/민감정보 암호화(토큰/OTP)
- [ ] 테스트 코드 작성

---

이 문서는 프로젝트의 베이스라인을 정의하며, 실제 구현 시 최소한 위 항목은 포함되어야 합니다.
