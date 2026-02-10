# NOTICE: Spring Web 개발 시 구현해야 할 핵심 항목

이 문서는 `db/init/schema.sql` 기준으로, Spring Web 프로젝트에서 구현해야 하는 기능을 체계적으로 정리한 개발 체크리스트입니다.

## 1. 인증/회원 기능

### 1.1 회원가입/로그인
- 로컬 회원가입(아이디/비밀번호 기반)
- 소셜 회원가입(OAuth2: Google/Kakao/GitHub 등)
- 로컬/소셜 계정 병합 정책(동일 이메일 처리)

### 1.1-1 2차 인증(2FA)
- 이메일/휴대폰 2차 인증 지원
- OTP 또는 코드 기반 인증 플로우 제공
- 2차 인증 코드 발급/만료/재전송 정책 정의

### 1.2 JWT 토큰 발급
- Access Token / Refresh Token 발급
- JWT Claim 설계(사용자 ID, 역할, 그룹 정보 등)
- Refresh Token 재발급(ROTATION 정책 여부 결정)

### 1.3 토큰 저장/검증
- Redis에 Access/Refresh 토큰 저장
- 토큰 폐기/강제 로그아웃 처리
- 중복 접속 차단 정책 적용(단일 세션 또는 N개 허용)

### 1.4 사용자 상태 관리
- 이메일/휴대폰 인증 처리 (`email_verified`, `phone_verified`)
- 잠금 정책 (`login_fail_count`, `is_locked`, `locked_at`)
- 계정 상태 변경 (`user_status`)

### 1.5 페이지/리소스별 2FA 정책
- 특정 페이지/기능 접근 시 2FA 요구 정책 정의
- 2FA 필요 여부 판단 로직(권한 체크 이전/이후) 결정
- 2FA 성공 시 일정 기간 재요구 여부(세션 단위/시간 단위) 결정

## 2. 감사 로깅 (필수)

### 2.1 감사 로그 기록
- 로그인/로그아웃/토큰 발급/폐기 기록 (`tb_audit_logs`)
- 권한 변경/모듈 생성/그룹 가입 등 주요 행위 기록
- 실패 이벤트도 기록 (예: 로그인 실패)

### 2.2 감사 로그 필드 기준
- 행위자 (actor_user_id, actor_type)
- 대상 정보 (target_type, target_id)
- 결과 (result_status)
- IP / User-Agent / 메타데이터

## 3. 그룹/조직 관리

### 3.1 그룹 생성 및 소속
- 그룹 생성/삭제
- 그룹 소유자 변경/관리자 정책
- 그룹 멤버 초대(`tb_group_invites`) 및 가입 처리

### 3.2 그룹 역할
- 그룹 내 역할 부여 (`tb_group_member_roles`)
- 그룹 단위 권한 부여 (`tb_group_roles`)

## 4. 모듈/인스턴스 관리

### 4.1 모듈 관리
- 모듈 정의 등록 (`tb_modules`)
- 모듈 권한 등록 (`tb_module_permissions`)

### 4.2 모듈 인스턴스 관리
- 인스턴스 생성/삭제 (`tb_module_instances`)
- 소유자 관리 (`owner_id`, `instance_type`)
- 인스턴스 활성/비활성 처리 (`enabled`)

## 5. 권한 체크 로직

### 5.1 권한 체크 흐름(권장)
1. 전역 역할(`tb_user_roles`) 확인
2. 모듈 인스턴스 소유자 여부 확인
3. 사용자 직접 권한(`tb_user_module_permissions`) 확인
4. 그룹 권한(`tb_group_module_permissions`) 확인

### 5.2 권한 체크 구현 위치
- Spring Security `AccessDecisionVoter` 또는 `MethodSecurity`
- 서비스 레벨 유틸리티로 공통화

## 6. 시스템 설정

### 6.1 기능 플래그
- 소셜 로그인 사용 여부 (`tb_system_settings`)
- 가입 허용 여부 (`tb_system_settings`)
- 2차 인증 필수 여부 및 적용 범위 (`tb_system_settings`)

### 6.2 설정 적용 방식
- 애플리케이션 시작 시 캐시 로딩
- 관리 API를 통한 실시간 수정

## 7. API 설계 (필수 컨트롤러)

### 7.1 인증/회원
- POST `/auth/signup`
- POST `/auth/login`
- POST `/auth/refresh`
- POST `/auth/logout`

### 7.2 그룹
- POST `/groups`
- GET `/groups/{id}`
- POST `/groups/{id}/invites`
- POST `/groups/{id}/members`

### 7.3 모듈
- POST `/modules`
- GET `/modules`
- POST `/modules/{code}/instances`
- GET `/modules/{code}/instances/{instanceId}`

### 7.4 권한
- POST `/permissions/grant`
- POST `/permissions/revoke`
- GET `/permissions/check`

## 8. 기타 고려사항

- 트랜잭션 처리 범위 정의
- FK 제약 위반 대비 예외 처리
- DB 마이그레이션 도입(Flyway 권장)
- 보안/민감정보 암호화(토큰/OTP)
- 멀티 프로젝트 구조 설계 (코어/관리자 API/사용자 API)

## 9. 멀티 프로젝트 구조(권장)

Spring 프로젝트를 기능별로 분리하여 코드 중복을 최소화합니다.

구성 예시:
- `core` : 공통 도메인/엔티티/리포지토리/권한 체크 로직
- `admin-api` : 관리자 기능(API), 시스템 설정/감사로그 관리
- `user-api` : 사용자 기능(API), 로그인/회원/모듈 접근

핵심 원칙:
- 모든 공통 로직은 `core`에 위치
- API 프로젝트는 비즈니스 로직을 최소화하고, `core` 서비스를 호출
- 인증/인가 필터, 감사 로깅, 예외 핸들링은 공통 모듈에서 재사용

---

이 문서는 프로젝트의 베이스라인을 정의하며, 실제 구현 시 최소한 위 항목은 포함되어야 합니다.
