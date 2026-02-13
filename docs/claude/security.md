# 보안 가이드라인

## 인증 아키텍처

### 인증 흐름

```
[로그인 요청] → 자격증명 검증 → JWT 발급 (Access + Refresh)
                                  → Redis에 토큰 저장
                                  → DB에 세션 감사 기록
                                  → 클라이언트에 토큰 반환

[API 요청] → JWT 필터 → 토큰 유효성 검증 (서명 + 만료)
                       → Redis에서 폐기 여부 확인
                       → SecurityContext에 인증 정보 설정
                       → 컨트롤러 진입
```

### JWT 설계

| 항목 | 값 |
|------|-----|
| Access Token 만료 | 30분 (설정 가능) |
| Refresh Token 만료 | 7일 (설정 가능) |
| 알고리즘 | HS256 또는 RS256 |
| 저장 위치 | Access: 메모리/헤더, Refresh: HttpOnly Cookie |

### Access Token Claims (예정)

```json
{
  "sub": "{user_id}",
  "roles": ["ROLE_USER"],
  "groups": ["group-uuid-1"],
  "iat": 1700000000,
  "exp": 1700001800
}
```

### 토큰 갱신 정책
- Refresh Token Rotation: 갱신 시 새 Refresh Token 발급, 기존 토큰 폐기
- 탈취 감지: 폐기된 Refresh Token 사용 시 해당 사용자의 모든 토큰 무효화

## 인증 방식

### 로컬 로그인
- `user_id` + `password` 기반
- `password_hash`: BCrypt (cost factor 12)
- 로그인 실패 시 `login_fail_count` 증가
- 5회 연속 실패 시 계정 잠금 (`is_locked = true`)
- 잠금 해제: 관리자 또는 일정 시간 후 자동 해제

### 소셜 로그인 (OAuth2)
- `tb_auth_providers`에 등록된 제공자만 허용
- OAuth2 콜백 → `tb_user_identities`에서 매핑 조회
- 기존 계정 있으면 연결, 없으면 신규 생성
- 동일 이메일 병합 정책 적용

### 2FA/OTP
- `tb_users.is_otp_use` 활성화된 사용자에게 적용
- TOTP 기반 (Google Authenticator 호환)
- `otp_secret`은 암호화하여 저장
- 시스템 설정으로 2FA 필수 범위 조절 가능

## 인가 (Authorization)

### Spring Security 설정 원칙

```java
// admin-api: 관리자 전용 설정
@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {
    // 모든 엔드포인트에 ROLE_ADMIN 필요
    // /actuator/health는 permitAll
}

// user-api: 사용자 전용 설정
@Configuration
@EnableWebSecurity
public class UserSecurityConfig {
    // /auth/** 는 permitAll
    // 나머지는 인증 필요
}
```

### 권한 체크 계층

1. **URL 레벨**: Spring Security `SecurityFilterChain`
2. **메서드 레벨**: `@PreAuthorize`, `@Secured`
3. **비즈니스 레벨**: 서비스 내 권한 체크 유틸리티

### 모듈 권한 체크 흐름

```
1. 전역 역할 (tb_user_roles) → SUPER_ADMIN이면 즉시 허용
2. 모듈 인스턴스 소유자 확인 → 소유자면 전체 권한
3. 사용자 직접 권한 (tb_user_module_permissions) 확인
4. 소속 그룹 권한 (tb_group_module_permissions) 합산
5. 직접 + 그룹 = additive (합산하여 최종 판단)
```

## 입력 검증

### 검증 계층

| 계층 | 방법 | 담당 |
|------|------|------|
| 컨트롤러 | `@Valid` + Bean Validation | 형식 검증 (필수, 길이, 이메일 등) |
| 서비스 | 비즈니스 규칙 검증 | 중복 체크, 존재 여부, 권한 확인 |
| 리포지토리 | DB 제약 조건 | UNIQUE, FK, NOT NULL |

### 검증 실패 응답
- Bean Validation 실패 → `GlobalExceptionHandler` → `COM_002` (400)
- 비즈니스 규칙 위반 → `BusinessException` → 해당 ErrorCode

### XSS 방지
- 입력값에 HTML 태그 허용하지 않음
- 출력 시 이스케이프 처리
- Content-Type 헤더 강제: `application/json`

### SQL Injection 방지
- JPA/JPQL 파라미터 바인딩 사용 (`:param`)
- 네이티브 쿼리 시에도 반드시 파라미터 바인딩
- 문자열 연결로 쿼리 생성 금지

## 민감 정보 처리

### 암호화/해싱 대상

| 데이터 | 처리 방법 |
|--------|----------|
| 비밀번호 | BCrypt 해싱 (복호화 불가) |
| OTP Secret | AES 양방향 암호화 |
| JWT Secret Key | 환경 변수로 주입 |
| Refresh Token | Redis 저장, DB에는 해시값 |

### 환경 변수 관리
- `application.properties`에 민감 정보 직접 기입 금지
- 프로필별 설정 분리: `application-local.properties`, `application-prod.properties`
- Docker 환경: `docker-compose.yml`의 `environment` 또는 `.env` 파일 사용
- `.env` 파일은 `.gitignore`에 포함

## 응답 헤더 보안

```java
// Spring Security 기본 보안 헤더 활성화
// X-Content-Type-Options: nosniff
// X-Frame-Options: DENY
// X-XSS-Protection: 0 (CSP로 대체)
// Content-Security-Policy: 필요 시 설정
// Strict-Transport-Security: 운영 환경에서 활성화
```

## CORS 정책

```java
// 개발 환경: 프론트엔드 로컬 서버 허용
// 운영 환경: 특정 도메인만 허용
// credentials: true (쿠키 전송 허용 — Refresh Token)
```

## 감사 로깅

### 필수 기록 대상
- 로그인 성공/실패
- 로그아웃
- 토큰 발급/갱신/폐기
- 권한 변경 (부여/회수)
- 계정 잠금/해제
- 시스템 설정 변경

### 기록 필드 (`tb_audit_logs`)
- `actor_user_id`: 행위자
- `actor_type`: 행위자 유형 (USER/SYSTEM/ADMIN)
- `target_type`: 대상 유형 (USER/GROUP/MODULE 등)
- `target_id`: 대상 ID
- `action`: 행위 (LOGIN/LOGOUT/GRANT/REVOKE 등)
- `result_status`: 결과 (SUCCESS/FAILURE)
- `ip_address`, `user_agent`: 접속 정보
- `metadata`: 추가 정보 (JSON)
