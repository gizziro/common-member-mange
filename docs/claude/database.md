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
| 모듈 | `tb_modules` | 모듈 정의 (SINGLE/MULTI) + slug |
| | `tb_module_instances` | 모듈 인스턴스 + slug |
| | `tb_module_permissions` | 모듈별 리소스-액션 정의 |
| 권한 부여 | `tb_user_module_permissions` | 사용자→모듈 권한 |
| | `tb_group_module_permissions` | 그룹→모듈 권한 |
| 전역 RBAC | `tb_resources` | 리소스 정의 |
| | `tb_permissions` | 리소스별 권한 |
| | `tb_roles` | 역할 (GLOBAL/GROUP) |
| | `tb_role_permissions` | 역할-권한 매핑 |
| | `tb_user_roles` | 사용자-역할 매핑 |
| | `tb_group_roles` | 그룹-역할 매핑 |
| | `tb_group_member_roles` | 그룹 멤버-역할 매핑 |
| 메뉴 | `tb_menus` | 네비게이션 메뉴 트리 |
| 시스템 | `tb_system_settings` | 기능 플래그 |
| | `tb_audit_logs` | 감사 로그 |

### 스키마 변경 예정 사항

모듈 시스템 (slug 라우팅, 3단계 권한, 메뉴 관리) 을 위해 아래 변경이 필요:

#### tb_modules에 slug 컬럼 추가
```sql
slug      VARCHAR(50)     NOT NULL,                     -- URL slug (예: board, wiki)
-- UNIQUE KEY uq_modules_slug (slug)
```

#### tb_module_instances에 slug 컬럼 추가
```sql
slug      VARCHAR(50)     NOT NULL,                     -- URL slug (예: notice, free-board)
-- UNIQUE KEY uq_module_instances_slug (module_code, slug)
```

#### tb_module_permissions에 resource 컬럼 추가

기존 `(module_code, action)` 구조에서 `(module_code, resource, action)` 3단계로 확장:

```sql
-- 변경 전
module_code   VARCHAR(50)     NOT NULL,                     -- 모듈 코드
action        VARCHAR(30)     NOT NULL,                     -- 권한 액션
-- UNIQUE KEY (module_code, action)

-- 변경 후
module_code   VARCHAR(50)     NOT NULL,                     -- 모듈 코드
resource      VARCHAR(50)     NOT NULL,                     -- 모듈 내 리소스 (post, comment 등)
action        VARCHAR(30)     NOT NULL,                     -- 권한 액션 (read, write, delete 등)
name          VARCHAR(100)    NOT NULL,                     -- 권한 표시명 (게시글 작성 등)
-- UNIQUE KEY uq_module_permissions (module_code, resource, action)
```

예시 데이터:

| module_code | resource | action | name |
|-------------|----------|--------|------|
| board | post | read | 게시글 읽기 |
| board | post | write | 게시글 작성 |
| board | post | modify | 게시글 수정 |
| board | post | delete | 게시글 삭제 |
| board | post | reply | 게시글 답글 |
| board | comment | read | 댓글 읽기 |
| board | comment | write | 댓글 작성 |
| board | comment | modify | 댓글 수정 |
| board | comment | delete | 댓글 삭제 |
| board | comment | anonymous | 댓글 익명 작성 |

런타임 권한 문자열: `UPPER(module_code) + "_" + UPPER(resource) + "_" + UPPER(action)`
→ `BOARD_POST_WRITE`, `BOARD_COMMENT_ANONYMOUS`

#### tb_menus 테이블 신규

```sql
CREATE TABLE tb_menus (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),   -- 메뉴 PK
  parent_id             CHAR(36)        NULL,                           -- 부모 메뉴 FK (NULL이면 최상위)
  name                  VARCHAR(100)    NOT NULL,                       -- 메뉴 표시명
  icon                  VARCHAR(50)     NULL,                           -- 아이콘 식별자
  menu_type             VARCHAR(20)     NOT NULL DEFAULT 'MODULE',      -- 메뉴 유형 (MODULE/LINK/SEPARATOR)
  module_instance_id    VARCHAR(50)     NULL,                           -- 연결된 모듈 인스턴스 FK (MODULE 타입)
  custom_url            VARCHAR(500)    NULL,                           -- 커스텀 링크 URL (LINK 타입)
  required_role         VARCHAR(50)     NULL,                           -- LINK 타입 가시성 제어용 역할
  sort_order            INT             NOT NULL DEFAULT 0,             -- 정렬 순서
  is_visible            TINYINT(1)      NOT NULL DEFAULT 1,             -- 관리자 수동 노출 제어
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_menus_parent
    FOREIGN KEY (parent_id) REFERENCES tb_menus(id) ON DELETE CASCADE,
  CONSTRAINT fk_menus_module_instance
    FOREIGN KEY (module_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE SET NULL,
  KEY idx_menus_parent_id (parent_id),
  KEY idx_menus_sort_order (sort_order)
);
```

#### 메뉴 유형 (menu_type) 및 가시성 규칙

| 유형 | 설명 | 가시성 결정 |
|----|------|------------|
| `MODULE` | 모듈 인스턴스에 연결, URL은 slug에서 자동 생성 | 연결된 인스턴스에 `*_*_READ` 권한 있으면 표시 |
| `LINK` | 커스텀 URL 직접 지정 | `required_role`로 제어 (NULL이면 전체 공개) |
| `SEPARATOR` | 구분선/그룹 헤더, 클릭 불가 | 하위 메뉴 중 하나라도 보이면 표시 |

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
