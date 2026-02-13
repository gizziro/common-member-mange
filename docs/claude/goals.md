# 프로젝트 목표

## 시스템 개요

RBAC 기반 회원/그룹/모듈 권한 관리 시스템. 관리자와 사용자 API를 분리하여 각각 독립적으로 배포·운영할 수 있는 구조.

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

### 4. 모듈 권한 시스템
- SINGLE 모듈: 시스템 전체에서 하나만 존재
- MULTI 모듈: 인스턴스 생성으로 운영 (예: 게시판)
- 모듈별 액션 정의 (create/read/update/delete)
- 사용자/그룹 단위 권한 부여

### 5. 전역 RBAC
- 리소스/권한/역할 계층 구조
- GLOBAL scope: 시스템 전체 역할
- GROUP scope: 그룹 내 역할
- 사용자 직접 권한 + 그룹 권한 합산 (additive)

### 6. 감사 로깅
- 모든 주요 행위 기록 (로그인, 권한 변경, 모듈 생성 등)
- 실패 이벤트 포함
- 행위자/대상/결과/IP/메타데이터 기록

### 7. 시스템 설정
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

### admin-api 모듈
- 시스템 설정 관리 API
- 사용자/그룹/모듈 관리 API (CRUD)
- 감사 로그 조회 API
- 권한/역할 관리 API
- 관리자 전용 Spring Security 설정

### user-api 모듈
- 회원가입/로그인/로그아웃 API
- 프로필 관리 API
- 그룹 가입/초대 처리 API
- 모듈 접근/인스턴스 생성 API
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
- [ ] 감사 로깅 구현
- [ ] 프론트엔드 개발
