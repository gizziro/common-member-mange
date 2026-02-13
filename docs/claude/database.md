# 데이터베이스 규칙

## 스키마 개요

MySQL 8.0 기반, 23개 테이블로 구성. 전체 스키마는 `db/init/schema.sql`에 정의.

### 테이블 분류

| 분류 | 테이블 | 설명 |
|------|--------|------|
| 사용자 | `tb_users` | 핵심 계정 정보 |
| | `tb_auth_providers` | OAuth2 제공자 정의 |
| | `tb_user_identities` | 사용자-소셜 계정 연결 |
| | `tb_sessions` | 세션/토큰 감사 기록 |
| 그룹 | `tb_groups` | 그룹 정보 |
| | `tb_group_members` | 그룹-사용자 매핑 |
| | `tb_group_invites` | 그룹 초대 |
| 모듈 | `tb_modules` | 모듈 정의 (SINGLE/MULTI) |
| | `tb_module_instances` | 모듈 인스턴스 |
| | `tb_module_permissions` | 모듈별 액션 정의 |
| 권한 부여 | `tb_user_module_permissions` | 사용자→모듈 권한 |
| | `tb_group_module_permissions` | 그룹→모듈 권한 |
| 전역 RBAC | `tb_resources` | 리소스 정의 |
| | `tb_permissions` | 리소스별 권한 |
| | `tb_roles` | 역할 (GLOBAL/GROUP) |
| | `tb_role_permissions` | 역할-권한 매핑 |
| | `tb_user_roles` | 사용자-역할 매핑 |
| | `tb_group_roles` | 그룹-역할 매핑 |
| | `tb_group_member_roles` | 그룹 멤버-역할 매핑 |
| 시스템 | `tb_system_settings` | 기능 플래그 |
| | `tb_audit_logs` | 감사 로그 |

## 네이밍 컨벤션

### 테이블

| 규칙 | 예시 |
|------|------|
| 접두사 `tb_` 사용 | `tb_users`, `tb_groups` |
| 복수형 명사 | `tb_users` (O), `tb_user` (X) |
| snake_case | `tb_group_members` |

### 컬럼

| 규칙 | 예시 |
|------|------|
| snake_case | `user_id`, `created_at` |
| PK: `id` (CHAR(36), UUID) | `id CHAR(36) PRIMARY KEY DEFAULT (UUID())` |
| FK: `{참조테이블 단수}_id` | `user_id`, `group_id` |
| boolean: `is_` 접두사 | `is_locked`, `is_otp_use` |
| 일시: `_at` 접미사 | `created_at`, `locked_at` |
| 상태: `_status` 접미사 | `user_status`, `invite_status` |

### 공통 컬럼 (Audit)

모든 테이블에 포함:
```sql
created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

## SQL 스타일

### 열 정렬 (Column-aligned) — 필수

```sql
-- 좋은 예: 열 정렬
CREATE TABLE tb_users (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
  user_id               VARCHAR(50)     NOT NULL,
  username              VARCHAR(100)    NOT NULL,
  email                 VARCHAR(320)    NOT NULL,
  is_locked             TINYINT(1)      NOT NULL DEFAULT 0,
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 나쁜 예: 정렬 없음
CREATE TABLE tb_users (
  id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
  user_id VARCHAR(50) NOT NULL,
  username VARCHAR(100) NOT NULL
);
```

### DDL 규칙
- `CREATE TABLE` 시 컬럼별 한 줄
- 인라인 주석으로 컬럼 설명 (`-- 사용자 PK`)
- FK 제약은 테이블 하단에 별도 정의
- `UNIQUE`, `INDEX`는 명시적으로 이름 부여

## JPA 엔티티 매핑 규칙

### 엔티티 기본 구조

```java
@Entity
@Table(name = "tb_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    // PK (UUID, DB에서 자동 생성)
    @Id
    @Column(name = "id", length = 36)
    private String id;

    // 로그인 ID
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    // 사용자 이름
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    // 생성 일시 (DB에서 자동 설정)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 일시 (DB에서 자동 갱신)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### 매핑 규칙

| 규칙 | 설명 |
|------|------|
| `@Table(name = "tb_xxx")` | 테이블명 명시 |
| `@Column(name = "xxx")` | 컬럼명 명시 (snake_case) |
| `@NoArgsConstructor(access = PROTECTED)` | JPA 기본 생성자 |
| `@Setter` 사용 금지 | 비즈니스 메서드로 상태 변경 |
| `@Enumerated(EnumType.STRING)` | Enum은 문자열 저장 |
| 연관관계는 지연 로딩 기본 | `@ManyToOne(fetch = LAZY)` |

### PK 전략
- MySQL UUID 함수 사용 (`DEFAULT (UUID())`)
- JPA에서는 `@GeneratedValue` 미사용, DB에서 생성
- 타입: `String` (CHAR(36))

## 마이그레이션 전략

- 현재: `db/init/schema.sql` + `db/init/data.sql`로 초기화 (Docker 첫 실행 시)
- 향후: Flyway 도입 예정
  - 마이그레이션 파일: `V{버전}__{설명}.sql` (예: `V2__add_user_nickname.sql`)
  - `db/init/`은 Docker 개발환경 전용으로 유지

## 쿼리 작성 규칙

- Spring Data JPA 메서드 쿼리 우선 사용
- 복잡한 쿼리는 `@Query` + JPQL
- 동적 쿼리 필요 시 QueryDSL 도입 고려
- N+1 문제 방지: `@EntityGraph` 또는 `JOIN FETCH` 활용
