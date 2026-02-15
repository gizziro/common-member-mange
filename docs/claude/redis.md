# Redis 키 규칙

## 개요

Redis는 JWT 토큰의 활성 상태 관리에 사용된다. JWT 서명 검증만으로는 로그아웃된 토큰을 무효화할 수 없으므로, Redis에 활성 세션 정보를 저장하여 이중 검증을 수행한다.

## 연결 정보

| 항목 | 로컬 개발 | Docker |
|------|----------|--------|
| Host | `localhost` | `redis` |
| Port | `6379` | `6379` |
| DB | `0` (기본) | `0` (기본) |

## 키 네이밍 규칙

### 인증 토큰

| 키 패턴 | 값 | TTL | 용도 |
|---------|-----|-----|------|
| `auth:access:{userPk}:{sessionId}` | `"active"` | Access Token 만료시간 (기본 30분) | 유효한 Access Token 세션 |
| `auth:refresh:{userPk}:{sessionId}` | `"active"` | Refresh Token 만료시간 (기본 7일) | 유효한 Refresh Token 세션 |

### 키 구성 요소

| 요소 | 설명 | 예시 |
|------|------|------|
| `auth` | 인증 네임스페이스 접두사 | - |
| `access` / `refresh` | 토큰 타입 | - |
| `{userPk}` | 사용자 PK (UUID) | `550e8400-e29b-41d4-a716-446655440000` |
| `{sessionId}` | 세션 PK (UUID) | `6ba7b810-9dad-11d1-80b4-00c04fd430c8` |

### 키 예시

```
auth:access:550e8400-e29b-41d4-a716-446655440000:6ba7b810-9dad-11d1-80b4-00c04fd430c8
auth:refresh:550e8400-e29b-41d4-a716-446655440000:6ba7b810-9dad-11d1-80b4-00c04fd430c8
```

## 토큰 생명주기

### 로그인
1. JWT Access + Refresh Token 생성
2. `auth:access:{userPk}:{sessionId}` = `"active"` (TTL: 30분)
3. `auth:refresh:{userPk}:{sessionId}` = `"active"` (TTL: 7일)

### API 요청 (JWT 필터)
1. JWT 서명 + 만료 검증
2. `auth:access:{userPk}:{sessionId}` 키 존재 확인
3. 둘 다 통과해야 인증 성공

### 토큰 갱신 (Refresh Token Rotation)
1. Refresh Token JWT 검증
2. `auth:refresh:{userPk}:{sessionId}` 존재 확인
3. 기존 access + refresh 키 삭제
4. 새 access + refresh 키 저장

### 로그아웃
1. `auth:access:{userPk}:{sessionId}` 삭제
2. `auth:refresh:{userPk}:{sessionId}` 삭제
3. 즉시 무효화 (다음 요청부터 인증 실패)

### 전체 로그아웃 (보안 사고 대응)
1. `SCAN auth:access:{userPk}:*` → 일괄 삭제
2. `SCAN auth:refresh:{userPk}:*` → 일괄 삭제

## 향후 확장 키 패턴 (예정)

| 키 패턴 | 용도 |
|---------|------|
| `otp:{userPk}:{code}` | OTP 인증 코드 (5분 TTL) |
| `email-verify:{token}` | 이메일 인증 토큰 (24시간 TTL) |
| `rate-limit:login:{ip}` | 로그인 시도 횟수 제한 (1분 TTL) |
| `cache:settings` | 시스템 설정 캐시 |
