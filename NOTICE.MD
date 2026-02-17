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
- [x] 모듈 정의 등록 (`tb_modules` — slug, description, is_enabled, updated_at 추가)
- [x] 모듈 권한 등록 (`tb_module_permissions` — resource 컬럼 추가, 3단계 권한 모델)
- [x] ModuleDefinition 인터페이스 구현
- [x] ModuleRegistry (모듈 자동 발견/등록, @Order(2) ApplicationRunner)
- [x] ModuleSchemaInitializer (모듈별 schema.sql 자동 실행, @Order(1) ApplicationRunner)
- [x] SlugResolver (slug → 모듈/인스턴스 매핑)
- [x] ModuleContentProvider / ModuleContentRegistry (SPI 패턴, 모듈 간 콘텐츠 참조)
- [x] ModuleErrorCode (9개 에러 코드)

### 4.2 모듈 인스턴스 관리
- [ ] 인스턴스 생성/삭제 (`tb_module_instances`)
- [ ] 소유자 관리 (`owner_id`, `instance_type`)
- [ ] 인스턴스 활성/비활성 처리 (`enabled`)
- [x] Slug 기반 동적 라우팅 (GET /resolve/{module-slug}, GET /resolve/{module-slug}/{instance-slug})
- [x] 별칭(Alias) fallback 해석 (SlugResolver.resolveByAlias — 모듈 slug 미매칭 시 메뉴 alias_path로 재시도)

### 4.3 페이지 모듈 (module-page)
- [x] PageModuleDefinition — SINGLE 타입 모듈 (code=page, slug=page)
- [x] 페이지 CRUD (tb_page_pages: slug, title, content, content_type, is_published)
- [x] 콘텐츠 유형: HTML / MARKDOWN / TEXT
- [x] Admin API: POST/GET/GET/{id}/PUT/{id}/DELETE/{id}/PATCH/{id}/publish
- [x] User API: GET /pages/{slug} (공개 페이지), GET /pages (공개 목록)
- [x] 모듈 스키마 자동 초기화 (db/page-schema.sql)
- [x] 권한 API: GET /pages/permissions, GET/PUT /pages/permissions/groups (그룹별 권한 매트릭스)

## 5. 권한 체크 로직

### 5.1 권한 체크 흐름(권장)
1. [ ] 전역 역할(`tb_user_roles`) 확인
2. [x] 모듈 인스턴스 소유자 여부 확인 (PermissionChecker.isOwner)
3. [x] 사용자 직접 권한(`tb_user_module_permissions`) 확인 (PermissionChecker.collectAllPermissionIds)
4. [x] 그룹 권한(`tb_group_module_permissions`) 확인 (PermissionChecker.collectAllPermissionIds — 합산)

### 5.2 권한 체크 구현 위치
- [x] PermissionChecker 유틸리티 (플랫 문자열 기반: BOARD_POST_WRITE, 소유자/직접/그룹 합산)
- [ ] @RequirePermission 어노테이션

## 6. 메뉴 관리 (URL 단축/Alias 시스템)

> **핵심 개념**: 메뉴는 "경로 단축/변환" 용도. 모든 보이는 메뉴는 인증 없이 공개.
> 권한 관리는 각 기능 모듈 admin 컨트롤러에서 담당.

### 6.1 메뉴 CRUD (admin-api)
- [x] 메뉴 항목 생성/수정/삭제 (POST/PUT/DELETE /menus)
- [x] 메뉴 트리 전체 조회 (GET /menus)
- [x] 메뉴 정렬 순서 변경 (PATCH /menus/{id}/order)
- [x] 메뉴 가시성 토글 (PATCH /menus/{id}/toggle)
- [x] 메뉴 유형: MODULE / LINK / SEPARATOR
- [x] 모듈/인스턴스 목록 조회 API (GET /menus/modules, GET /menus/modules/{code}/instances)

### 6.2 메뉴 Alias 시스템
- [x] alias_path: 외부 단축 경로 (UNIQUE, slug 형식)
- [x] content_path: SINGLE 모듈 콘텐츠 경로 (예: page 모듈 → "test" → /page/test)
- [x] Alias 유효성 검증: 형식/중복/모듈 slug 충돌/예약어(login, sign-up 등) 체크
- [x] MenuErrorCode 4개 추가 (ALIAS_DUPLICATE, ALIAS_CONFLICTS_MODULE, ALIAS_RESERVED, ALIAS_INVALID_FORMAT)

### 6.3 메뉴 조회 (user-api)
- [x] 공개 메뉴 트리 반환 (GET /menus/me — 인증 불필요, is_visible=true인 메뉴만)
- [x] SEPARATOR 타입: 하위 메뉴 중 하나라도 있으면 표시 (자식 없으면 자동 숨김)

## 7. 시스템 설정

### 7.1 기능 플래그
- [ ] 회원가입 활성화 여부 (`tb_system_settings`, 비활성 시 가입 API 차단)
- [ ] 회원가입 시 자동 인증 On/Off (`tb_system_settings`, On이면 가입 즉시 ACTIVE, Off면 PENDING)
- [ ] 소셜 로그인 사용 여부 (`tb_system_settings`)
- [ ] 2차 인증 필수 여부 및 적용 범위 (`tb_system_settings`)

### 7.2 관리자 페이지 접근 제어
- [x] admin-api 접근 시 administrator 그룹 소속 여부 검증 (AdminGroupFilter — JwtAuthenticationFilter 이후 실행)
- [x] 관리자 페이지 접근 가능한 사용자/그룹 관리 (AdminAccessService — administrator 그룹 PK 캐시)
- [x] 비관리자가 admin-api 로그인 시도 시 적절한 에러 응답 (AUTH_ACCESS_DENIED)

### 7.2-1 관리자 페이지 세분화 권한 (미구현 — 설계 검토 필요)

> **현재**: AdminGroupFilter로 administrator 그룹 소속 여부만 확인 → 모든 관리자가 모든 관리 기능 사용 가능
> **목표**: 관리자 페이지 내에서도 메뉴/기능별 접근 권한을 그룹·회원 단위로 제어

#### 배경
- 부 관리자 계정(또는 운영자 그룹)이 필요한 시점이 올 수 있음
- 예: "회원 관리"만 가능한 운영자, "시스템 설정"까지 가능한 슈퍼 관리자
- user-api 메뉴 권한 모델(MODULE 타입 메뉴 → 인스턴스 권한으로 가시성 파생)을 admin-api에도 적용하는 방향

#### 선행 작업 (구현 순서)
1. **모듈 시스템 프레임워크** — ModuleDefinition, ModuleRegistry, SlugResolver (§4.1)
2. **메뉴 관리 시스템** — tb_menus CRUD, 메뉴 트리 조회 (§6)
3. **PermissionChecker** — 플랫 문자열 기반 권한 체크 유틸리티 (§5)
4. **admin-api 메뉴 권한 통합** — 아래 설계안 참조

#### 설계 방향 (초안)

```
[현재 구조 — 1단계 게이트]
  AdminGroupFilter → administrator 그룹 소속? → 전체 허용 / 전체 차단

[목표 구조 — 2단계 게이트]
  1단계: AdminGroupFilter → admin 접근 허용 그룹 소속? (administrator + 부관리자 그룹 등)
  2단계: 컨트롤러/메뉴별 → PermissionChecker로 해당 기능 권한 확인
```

**방안 A — admin-api도 메뉴 시스템 공유**
- tb_menus에 admin용 메뉴도 등록 (menu_scope = 'ADMIN' 같은 구분 컬럼 추가 또는 별도 admin 메뉴 트리)
- admin 메뉴 항목에 모듈 인스턴스 연결 → 기존 권한 모델 그대로 적용
- 장점: user-api와 동일한 권한 모델 재사용, 일관성
- 단점: admin 기능(회원관리, 설정 등)을 모듈 인스턴스로 모델링해야 함

**방안 B — admin 전용 권한 테이블**
- admin 기능별 권한 코드 정의 (ADMIN_USER_READ, ADMIN_USER_WRITE, ADMIN_SETTING_WRITE 등)
- tb_admin_permissions 또는 기존 tb_module_permissions에 admin 모듈로 등록
- AdminGroupFilter를 확장하여 요청 URL → 필요 권한 매핑 → PermissionChecker 호출
- 장점: admin 특화 설계 가능, 메뉴 시스템과 독립
- 단점: user-api와 별도 권한 체계 관리 필요

**방안 C (권장) — 하이브리드**
- 1단계 게이트: AdminGroupFilter는 "admin 접근 가능 그룹 목록"으로 확장 (administrator 외 추가 그룹 허용)
- 2단계 게이트: admin 기능을 SINGLE 타입 모듈로 등록 (user-mgmt, group-mgmt, system-settings 등)
  → 기존 3단계 권한 모델(모듈→리소스→액션) 적용
  → 예: `ADMIN_USER_READ`, `ADMIN_USER_WRITE`, `ADMIN_SETTING_WRITE`
- 메뉴 시스템으로 admin 사이드바도 동적 렌더링
- 장점: 기존 인프라 최대 활용, user/admin 권한 모델 통일
- 단점: 모듈 시스템 + 메뉴 시스템 완성이 선행 필수

#### 결론
- 모듈 시스템(§4) → 메뉴 관리(§6) → PermissionChecker(§5) 순서로 구현 후 admin 권한 세분화 진행
- 현재 AdminGroupFilter는 그대로 유지 (1단계 게이트 역할)
- 상세 설계는 메뉴 관리 구현 시점에 확정

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
- [x] DELETE `/auth/oauth2/identities/{id}` — 소셜 연동 해제
- [x] POST `/auth/oauth2/set-password` — 소셜 전용 사용자 로컬 자격증명(ID+PW) 설정
- [x] DELETE `/auth/withdraw` — 사용자 본인 탈퇴

### 8.1-1 인증/회원 (admin-api)
- [x] POST `/auth/login` — 관리자 로그인
- [x] POST `/auth/logout` — 관리자 로그아웃
- [x] POST `/auth/refresh` — 관리자 토큰 갱신
- [x] GET `/auth/me` — 관리자 현재 사용자 정보
- [x] GET `/auth/providers` — 소셜 Provider 전체 목록
- [x] GET `/auth/providers/{id}` — Provider 상세 조회
- [x] PUT `/auth/providers/{id}` — Provider 설정 수정

### 8.1-2 회원 관리 (admin-api)
- [x] GET `/users` — 회원 목록 (페이지네이션)
- [x] GET `/users/{id}` — 회원 상세 조회
- [x] PUT `/users/{id}` — 회원 정보 수정 (이름/이메일/상태)
- [x] PUT `/users/{id}/password` — 회원 비밀번호 변경
- [x] DELETE `/users/{id}` — 회원 삭제 (자기 자신/관리자 그룹 소속 보호, Redis 토큰 정리)
- [x] PUT `/users/{id}/unlock` — 회원 잠금 해제
- [x] GET `/users/{id}/groups` — 회원 소속 그룹 목록
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

### 8.4 모듈 (부분 구현)
- [ ] POST `/modules` — 모듈 등록 (관리 API, 미구현)
- [ ] GET `/modules` — 모듈 목록 (관리 API, 미구현)
- [ ] POST `/modules/{code}/instances` — 인스턴스 생성 (미구현)
- [ ] GET `/modules/{code}/instances/{instanceId}` — 인스턴스 조회 (미구현)
- [x] GET `/resolve/{module-slug}` — SINGLE 모듈 해석 + 별칭 fallback (user-api)
- [x] GET `/resolve/{module-slug}/{instance-slug}` — MULTI 모듈 해석 + 별칭 fallback (user-api)

### 8.5 권한 (부분 구현)
- [ ] POST `/permissions/grant` — 범용 권한 부여 (미구현)
- [ ] POST `/permissions/revoke` — 범용 권한 회수 (미구현)
- [ ] GET `/permissions/check` — 범용 권한 확인 (미구현)
- [x] GET `/pages/permissions` — 페이지 모듈 권한 정의 목록 (admin-api)
- [x] GET `/pages/permissions/groups` — 페이지 모듈 그룹별 권한 현황 (admin-api)
- [x] PUT `/pages/permissions/groups` — 페이지 모듈 그룹별 권한 설정 (admin-api)

### 8.6 메뉴 (구현 완료)
- [x] POST `/menus` — 메뉴 생성 (admin-api)
- [x] GET `/menus` — 메뉴 트리 조회 (admin-api)
- [x] PUT `/menus/{id}` — 메뉴 수정 (admin-api)
- [x] DELETE `/menus/{id}` — 메뉴 삭제 (admin-api)
- [x] PATCH `/menus/{id}/order` — 메뉴 정렬 순서 변경 (admin-api)
- [x] PATCH `/menus/{id}/toggle` — 메뉴 가시성 토글 (admin-api)
- [x] GET `/menus/modules` — 모듈 목록 조회 (admin-api)
- [x] GET `/menus/modules/{code}/instances` — 모듈별 인스턴스 목록 (admin-api)
- [x] GET `/menus/me` — 공개 메뉴 트리 (user-api, 인증 불필요)

### 8.7 페이지 모듈 (구현 완료)
- [x] POST `/pages` — 페이지 생성 (admin-api)
- [x] GET `/pages` — 페이지 목록 (admin-api / user-api)
- [x] GET `/pages/{id}` — 페이지 상세 (admin-api)
- [x] PUT `/pages/{id}` — 페이지 수정 (admin-api)
- [x] DELETE `/pages/{id}` — 페이지 삭제 (admin-api)
- [x] PATCH `/pages/{id}/publish` — 공개/비공개 토글 (admin-api)
- [x] GET `/pages/{slug}` — 공개 페이지 조회 (user-api)

## 9. 프론트엔드

### 9.1 사용자 웹 (frontend/user)
- [x] 회원가입 페이지
- [x] 로그인 페이지 (소셜 로그인 버튼 포함)
- [x] 프로필 페이지 (소셜 계정 연동/해제, 비밀번호 설정, 회원 탈퇴)
- [x] 소개 페이지
- [x] 소셜 로그인 콜백 처리 페이지
- [x] 소셜 연동 확인 페이지 (기존 계정 발견 시 로컬 ID/PW 입력)
- [x] 소셜 연동 성공 콜백 페이지
- [x] OAuth2 콜백 에러 페이지 (에러 코드별 사용자 친화적 메시지 표시)
- [x] shadcn/ui 컴포넌트 전환
- [x] toast 알림 (sonner) 적용
- [x] 동적 메뉴 렌더링 (Header.tsx — 공개 메뉴 API 기반, aliasPath 우선 URL)
- [x] 동적 라우팅 ([...slug]/page.tsx — resolve API + 별칭 subPath 통합)
- [x] 페이지 뷰어 (PageViewer.tsx — HTML/Markdown/Text 렌더링)

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
- [x] 메뉴 관리 페이지 (메뉴 CRUD + alias/contentPath 입력 + 모듈/인스턴스 선택)
- [x] 페이지 관리 페이지 (페이지 CRUD + 공개/비공개 토글 + 그룹별 권한 매트릭스)
- [x] 페이지 편집 페이지 (/pages/[id] — 제목/슬러그/콘텐츠 편집)
- [ ] 모듈/인스턴스 관리 페이지
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
- [x] **admin-api 접근 제어: administrator 그룹 소속자만 허용 (AdminGroupFilter + AuthController 로그인/갱신 차단)**
- [ ] **OAuth2 콜백 리다이렉트 URL 외부화**: `OAuth2Controller`의 프론트엔드 리다이렉트 URL(`http://localhost:3020/auth/oauth2/...`)이 하드코딩됨. `application.yml`에 `app.frontend-url` 등의 프로퍼티로 추출하여 환경별(로컬/운영) 설정 가능하도록 리팩토링 필요
- [ ] 트랜잭션 처리 범위 정의
- [ ] FK 제약 위반 대비 예외 처리
- [ ] DB 마이그레이션 도입(Flyway 권장)
- [ ] 보안/민감정보 암호화(토큰/OTP)
- [ ] 테스트 코드 작성

---

이 문서는 프로젝트의 베이스라인을 정의하며, 실제 구현 시 최소한 위 항목은 포함되어야 합니다.
