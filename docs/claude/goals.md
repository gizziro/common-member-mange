# 프로젝트 목표

## 시스템 개요
- RBAC 기반 회원/그룹/모듈 권한 관리 시스템. 관리자와 사용자 API를 분리하여 각각 독립적으로 배포·운영할 수 있는 구조.


## 핵심 기능 목표

### 1. 인증 시스템
- 로컬 로그인 (user_id + password)
- 소셜 로그인 (OAuth2: Google, Kakao, GitHub 등)
- JWT 기반 인증 (Access Token + Refresh Token)
- Redis 토큰 저장 및 검증
- 2FA/OTP 지원 (이메일, 휴대폰, TOTP)
- 세션 감사 로깅 (IP, User-Agent, 디바이스)

### 2. 회원 관리
- 회원가입 (로컬/소셜)
- 동일 이메일 계정 병합 정책
- 이메일/휴대폰 인증
- 계정 잠금 정책 (로그인 실패 횟수 기반)
- 사용자 상태 관리 (PENDING → ACTIVE → SUSPENDED)

### 3. 그룹 관리
- 그룹 생성/삭제 (소유자 기반)
- 이메일 초대 (토큰 기반 만료 정책)
- 그룹 멤버 관리 및 역할 부여

### 4. 모듈 시스템
- SINGLE 모듈: 시스템 전체에서 하나만 존재 (예: 대시보드, 설정)
- MULTI 모듈: 인스턴스 생성으로 운영 (예: 게시판, 블로그, 가계부)
- **Slug 기반 동적 라우팅**: 모듈과 인스턴스에 slug를 부여하여 URL을 동적 구성
  - SINGLE: `/{module-slug}` (예: `/dashboard`)
  - MULTI: `/{module-slug}/{instance-slug}` (예: `/board/notice`)
- **기능 모듈은 독립 Gradle 프로젝트** (`modules/module-board`, `modules/module-blog` 등)
  - core의 `ModuleDefinition` 인터페이스 구현
  - 모듈별 엔티티/리포지토리/서비스/컨트롤러 자체 포함
  - `@ConditionalOnProperty(name = "app.api-type")` 으로 admin/user 컨트롤러 선택적 활성화
  - API 모듈의 `build.gradle` dependency 추가만으로 모듈 탑재

#### 3단계 권한 모델: 모듈 → 리소스 → 액션
- 모듈이 내부 리소스별 세분화된 액션을 정의
- 예시:
  ```
  board 모듈
    ├── post    → read, write, modify, delete, reply
    └── comment → read, write, modify, delete, anonymous

  blog 모듈
    ├── post    → read, write, modify, delete, publish
    └── comment → read, write, modify, delete

  가계부 모듈
    ├── transaction → read, write, modify, delete
    ├── category    → read, write, modify, delete
    └── report      → read, export
  ```
- **코드에서는 플랫 문자열로 체크**: `{MODULE}_{RESOURCE}_{ACTION}` (UPPER_SNAKE_CASE)
  - `BOARD_POST_WRITE`, `BOARD_COMMENT_ANONYMOUS`, `BLOG_POST_PUBLISH`
  - DB는 3컬럼(module_code, resource, action)으로 구조화, 런타임은 조합된 문자열로 비교
- 사용자/그룹 단위로 인스턴스별 권한 부여

### 5. 메뉴 관리 (모듈 인스턴스의 게이트웨이)
- **메뉴 = 모듈 인스턴스를 사용자에게 노출하고 권한을 제어하는 중앙 허브** (WordPress 방식)
- 관리자 설정 흐름:
  1. 모듈 인스턴스 생성 (예: "공지사항" 게시판)
  2. 메뉴 항목에 인스턴스 연결
  3. 인스턴스별 권한 설정 → 메뉴 가시성 자동 결정
- 트리 구조 (부모-자식 관계로 다단계 메뉴 지원)
- 메뉴 유형:
  - `MODULE`: 모듈 인스턴스 연결 — URL은 slug에서 자동 생성
  - `LINK`: 커스텀 URL 직접 지정
  - `SEPARATOR`: 구분선/그룹 헤더 (하위 메뉴 그룹핑)
- **메뉴 가시성 = 모듈 인스턴스 권한에서 파생**:
  - MODULE 타입: 연결된 인스턴스에 `*_*_READ` 권한이 있으면 표시
  - LINK 타입: `required_role`로 직접 제어
  - SEPARATOR 타입: 하위 메뉴 중 하나라도 보이면 표시
- 프론트엔드에서 메뉴 API 호출 → 동적 네비게이션 렌더링 + 각 메뉴별 권한 목록 포함

### 6. 전역 RBAC
- 리소스/권한/역할 계층 구조
- GLOBAL scope: 시스템 전체 역할
- GROUP scope: 그룹 내 역할
- 사용자 직접 권한 + 그룹 권한 합산 (additive)

### 7. 감사 로깅
- 모든 주요 행위 기록 (로그인, 권한 변경, 모듈 생성 등)
- 실패 이벤트 포함
- 행위자/대상/결과/IP/메타데이터 기록

### 8. 시스템 설정
- 기능 플래그 (소셜 로그인 허용, 가입 허용, 2FA 필수 등)
- 관리 API를 통한 실시간 설정 변경
- 애플리케이션 시작 시 캐시 로딩

## 모듈별 책임

### core 모듈
- 공통 도메인 엔티티 (JPA Entity)
- 공통 리포지토리 (Spring Data JPA)
- 공통 서비스 (권한 체크, 감사 로깅 등)
- 공통 예외 처리 (GlobalExceptionHandler, ErrorCode)
- 공통 응답 DTO (ApiResponse, ErrorDetail)
- Spring Security 공통 설정 (JWT 필터, 인증 프로바이더)
- **모듈 시스템 프레임워크** (ModuleDefinition 인터페이스, ModuleRegistry, SlugResolver)
- **메뉴 관리** (메뉴 엔티티, 메뉴 서비스, 권한 기반 메뉴 트리 조회)
- **PermissionChecker** (플랫 문자열 기반 권한 체크 유틸리티)

### 기능 모듈 (modules/)
- 각 모듈은 독립 Gradle 프로젝트 (core 참조)
- `ModuleDefinition` 구현으로 메타데이터 등록 (이름, slug, 리소스별 권한 정의)
- admin/user 컨트롤러를 자체 포함 (app.api-type 프로퍼티로 선택적 활성화)
- 예정: `module-board` (게시판), `module-blog` (블로그), `module-accounting` (가계부)

### admin-api 모듈
- 시스템 설정 관리 API
- 사용자/그룹/모듈 관리 API (CRUD)
- **메뉴 관리 API** (메뉴 트리 CRUD, 정렬, 모듈 인스턴스 연결)
- 감사 로그 조회 API
- 권한/역할 관리 API
- 관리자 전용 Spring Security 설정

### user-api 모듈
- 회원가입/로그인/로그아웃 API
- 프로필 관리 API
- 그룹 가입/초대 처리 API
- 모듈 접근/인스턴스 생성 API
- **메뉴 조회 API** (권한 기반 필터링된 메뉴 트리 반환)
- **Slug 기반 동적 라우팅 API** (slug로 모듈/인스턴스 조회)
- 사용자 전용 Spring Security 설정

## 권한 체크 흐름

```
요청 → JWT 검증 → 전역 역할 확인 → 모듈 소유자 확인
                                  → 사용자 직접 권한 확인
                                  → 그룹 권한 합산
                                  → 최종 허용/거부
```

1. 전역 역할 (`tb_user_roles`) 확인
2. 모듈 인스턴스 소유자 (`tb_module_instances.owner_id`) 확인
3. 사용자 직접 모듈 권한 (`tb_user_module_permissions`) 확인
4. 소속 그룹 모듈 권한 (`tb_group_module_permissions`) 합산
5. 사용자 직접 + 그룹 권한은 additive (합산)

## 현재 진행 상태

- [x] DB 스키마 설계 완료 (23개 테이블)
- [x] 정책 문서 작성 완료 (INFO.MD)
- [x] 멀티모듈 프로젝트 구조 구축
- [x] REST API 공통 기반 (ApiResponse, ErrorCode, GlobalExceptionHandler)
- [ ] JPA 엔티티 매핑
- [ ] Spring Security + JWT 인증
- [ ] Redis 연동
- [ ] 인증/회원 API 구현
- [ ] 그룹/모듈/권한 API 구현
- [ ] 메뉴 관리 시스템 구현 (slug 기반 동적 라우팅 포함)
- [ ] 감사 로깅 구현
- [ ] 프론트엔드 개발 (동적 메뉴 렌더링 포함)
