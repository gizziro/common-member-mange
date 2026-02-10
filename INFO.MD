# 회원/그룹/모듈/권한 관리 정책 안내

이 문서는 `db/init/schema.sql`에 정의된 스키마를 기준으로 인증/인가 및 모듈 권한 운영 정책을 설명합니다.

## 1. 계정/로그인 정책

### 1.1 사용자 기본 정보 (`users`)
- 내부 계정의 핵심 정보 테이블입니다.
- 로컬 로그인과 소셜 로그인 모두 지원합니다.

주요 컬럼 의미:
- `user_id`: 로그인 ID (로컬 로그인 시 사용)
- `password_hash`: 로컬 로그인용 비밀번호 해시
- `provider` / `provider_id`: 소셜 로그인 제공자 및 외부 사용자 식별자
- `email_verified`, `phone_verified`: 인증 완료 여부
- `is_locked`, `locked_at`, `login_fail_count`: 잠금 정책
- `user_status`: 사용자 상태 (`PENDING`, `ACTIVE`, `SUSPENDED` 등)

권장 정책:
- 로컬 로그인은 `user_id` + `password_hash`로 인증합니다.
- 소셜 로그인은 `provider` + `provider_id` 기반으로 사용자 매핑 후 로그인 처리합니다.

### 1.2 소셜 계정 연동 (`user_identities`)
- 하나의 사용자에 여러 소셜 계정을 연결할 수 있습니다.
- `provider_id`(FK) + `provider_subject`(외부 고유 ID)로 유니크 제약을 둡니다.

권장 정책:
- OAuth2 로그인 성공 시 `user_identities`에 연결 정보 저장
- 동일 이메일 병합 정책이 필요하면 별도의 규칙 수립

### 1.3 세션/토큰 감사 (`sessions`)
- JWT 기반 인증이더라도 감사 목적의 기록을 위해 사용합니다.
- Redis에 저장된 토큰과 별도로 최소 이력 저장이 가능합니다.

저장 항목:
- 토큰 해시, 만료시간, 발급시간
- 마지막 접속, 폐기 시각/사유
- IP, User-Agent, 디바이스 정보

권장 정책:
- 중복 접속 차단을 위해 `user_id + device_id` 기준 정책을 운영
- 강제 로그아웃/정책 위반 시 `revoked_at` 기록

## 2. 그룹 정책

### 2.1 그룹 (`groups`)
- 하나의 사용자가 그룹을 생성하고 소유합니다.
- `owner_user_id`가 그룹 소유자입니다.

### 2.2 그룹 구성원 (`group_members`)
- 그룹과 사용자 간의 다대다 관계를 정의합니다.
- 그룹 가입일(`joined_at`)을 기록합니다.

### 2.3 그룹 초대 (`group_invites`)
- 이메일 기반 초대 토큰 발급/만료 정책을 지원합니다.
- 초대 수락 시 해당 그룹에 멤버로 등록됩니다.

## 3. 모듈 정책

모듈은 서비스 내 기능 단위를 의미합니다. 단일 모듈과 복수 모듈 두 가지 형태가 있습니다.

### 3.1 모듈 (`modules`)
- `type`:
  - `SINGLE`: 서비스 전체에서 하나만 존재하는 모듈
  - `MULTI`: 인스턴스를 생성해서 운영하는 모듈 (예: 게시판)

### 3.2 모듈 인스턴스 (`module_instances`)
- `MULTI` 모듈의 실제 인스턴스를 저장합니다.
- 소유자에 따라 `owner_user_id` 또는 `owner_group_id`로 귀속됩니다.

권장 정책:
- `owner_type`에 따라 소유자가 반드시 하나만 지정되도록 애플리케이션 단에서 검증
- 인스턴스 생성 시 기본 권한을 소유자에게 부여

### 3.3 모듈 권한 (`module_permissions`)
- 모듈별로 가능한 액션을 정의합니다.
- 예: `board` 모듈에 `create/read/update/delete`

### 3.4 사용자/그룹 모듈 권한 부여
- `user_module_permissions`: 사용자에게 직접 부여된 모듈 인스턴스 권한
- `group_module_permissions`: 그룹에 부여된 모듈 인스턴스 권한

권장 정책:
- 권한 체크는 사용자 직접 권한 + 소속 그룹 권한을 합산하여 판단

## 4. 전역 역할(RBAC) 정책

### 4.1 리소스/권한 (`resources`, `permissions`)
- 서비스 전역의 기능 단위를 리소스로 정의합니다.
- 리소스마다 가능한 액션을 권한으로 정의합니다.

### 4.2 역할 (`roles`, `role_permissions`)
- 권한을 묶어 역할로 관리합니다.
- `scope`는 역할 적용 범위를 나타냅니다.
  - `GLOBAL`: 전역 역할
  - `GROUP`: 그룹 단위 역할

### 4.3 사용자/그룹 역할
- `user_roles`: 사용자 전역 역할
- `group_roles`: 그룹 전역 역할
- `group_member_roles`: 그룹 내 멤버 역할

권장 정책:
- 시스템 관리자 역할은 `GLOBAL` scope로 관리
- 그룹 관리자/멤버 역할은 `GROUP` scope로 관리

## 5. 권한 체크 흐름 (권장)

1. 로그인 사용자 확인
2. 전역 역할(`user_roles`)로 권한 판단
3. 그룹 소속 여부 및 그룹 역할 확인
4. 모듈 인스턴스 권한 확인 (`user_module_permissions` + `group_module_permissions`)
5. 소유자 권한 (`module_instances.owner_*`)에 따른 추가 권한 부여

## 6. 운영 팁

- 토큰 검증은 Redis 기준으로 수행하고, DB는 감사 및 정책 판단 기준으로 사용
- 불필요한 권한 중복은 최소화하고, 필요 시 정리 정책을 도입
- 모듈과 권한은 코드 레벨 정의 + DB 시드 동기화를 권장
