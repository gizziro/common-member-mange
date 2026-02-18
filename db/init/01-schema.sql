-- Schema initialization
-- WARNING: This is executed only on first container initialization (empty data dir)

-- 클라이언트 인코딩을 utf8mb4로 설정 (한국어 깨짐 방지)
SET NAMES utf8mb4;

-- Drop existing tables if re-running manually
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS tb_sms_logs;
DROP TABLE IF EXISTS tb_group_member_roles;
DROP TABLE IF EXISTS tb_group_roles;
DROP TABLE IF EXISTS tb_user_roles;
DROP TABLE IF EXISTS tb_role_permissions;
DROP TABLE IF EXISTS tb_roles;
DROP TABLE IF EXISTS tb_audit_logs;
DROP TABLE IF EXISTS tb_settings;
DROP TABLE IF EXISTS tb_page_pages;
DROP TABLE IF EXISTS tb_menus;
DROP TABLE IF EXISTS tb_group_module_permissions;
DROP TABLE IF EXISTS tb_user_module_permissions;
DROP TABLE IF EXISTS tb_module_permissions;
DROP TABLE IF EXISTS tb_module_instances;
DROP TABLE IF EXISTS tb_modules;
DROP TABLE IF EXISTS tb_permissions;
DROP TABLE IF EXISTS tb_resources;
DROP TABLE IF EXISTS tb_group_invites;
DROP TABLE IF EXISTS tb_group_members;
DROP TABLE IF EXISTS tb_groups;
DROP TABLE IF EXISTS tb_sessions;
DROP TABLE IF EXISTS tb_user_identities;
DROP TABLE IF EXISTS tb_auth_providers;
DROP TABLE IF EXISTS tb_sms_providers;
DROP TABLE IF EXISTS tb_users;
SET FOREIGN_KEY_CHECKS = 1;

-- Users (core account)
CREATE TABLE tb_users (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 사용자 PK
  user_id               VARCHAR(50)     NOT NULL,                     -- 로그인 ID
  username              VARCHAR(100)    NOT NULL,                     -- 사용자 이름
  provider              VARCHAR(20)     NOT NULL,                     -- 가입/로그인 제공자
  provider_id           VARCHAR(255)    NOT NULL,                     -- 소셜 제공자 사용자 ID
  password_hash         VARCHAR(255)    NOT NULL,                     -- 비밀번호 해시
  password_change_date  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 비밀번호 변경 일시
  email                 VARCHAR(320)    NOT NULL,                     -- 이메일 주소
  email_token           VARCHAR(255)    NULL,                         -- 이메일 인증 토큰
  email_verified        TINYINT(1)      NOT NULL DEFAULT 0,           -- 이메일 인증 여부
  phone                 VARCHAR(20)     NULL,                         -- 전화번호
  phone_verified        TINYINT(1)      NOT NULL DEFAULT 0,           -- 전화번호 인증 여부
  is_sms_agree          TINYINT(1)      NOT NULL DEFAULT 0,           -- SMS 수신 동의 여부
  social_join_verified  TINYINT(1)      NOT NULL DEFAULT 0,           -- 소셜 가입 인증 여부
  social_join_token     VARCHAR(255)    NULL,                         -- 소셜 가입 인증 토큰
  is_otp_use            TINYINT(1)      NOT NULL DEFAULT 0,           -- OTP 사용 여부
  otp_secret            VARCHAR(255)    NULL,                         -- OTP 비밀키
  login_fail_count      INT             NOT NULL DEFAULT 0,           -- 로그인 실패 횟수
  is_locked             TINYINT(1)      NOT NULL DEFAULT 0,           -- 잠금 여부
  locked_at             DATETIME        NULL,                         -- 잠금 일시
  user_status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',   -- 사용자 상태
  created_by            VARCHAR(100)    NOT NULL,                     -- 생성자
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
  updated_by            VARCHAR(100)    NULL,                         -- 수정자
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  UNIQUE KEY uq_users_user_id (user_id),
  UNIQUE KEY uq_users_email (email)
);

-- Auth providers (e.g., local, google, kakao, naver)
CREATE TABLE tb_auth_providers (
  id                CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 제공자 PK
  code              VARCHAR(50)     NOT NULL,                     -- 제공자 코드 (local, google, kakao, naver)
  name              VARCHAR(100)    NOT NULL,                     -- 제공자 표시명
  is_enabled        TINYINT(1)      NOT NULL DEFAULT 1,           -- 사용 여부
  client_id         VARCHAR(255)    NULL,                         -- OAuth2 Client ID
  client_secret     VARCHAR(255)    NULL,                         -- OAuth2 Client Secret
  redirect_uri      VARCHAR(500)    NULL,                         -- 인가 코드 콜백 URL
  authorization_uri VARCHAR(500)    NULL,                         -- 인가 엔드포인트
  token_uri         VARCHAR(500)    NULL,                         -- 토큰 교환 엔드포인트
  userinfo_uri      VARCHAR(500)    NULL,                         -- 사용자 정보 조회 엔드포인트
  scope             VARCHAR(500)    NULL,                         -- 요청 Scope (공백 구분)
  icon_url          VARCHAR(500)    NULL,                         -- 로그인 버튼 아이콘 URL
  display_order     INT             NOT NULL DEFAULT 0,           -- 로그인 버튼 표시 순서
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  UNIQUE KEY uq_auth_providers_code (code)
);

-- External identities for social login / provider linkage
CREATE TABLE tb_user_identities (
  id                CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 식별자 PK
  user_id           CHAR(36)        NOT NULL,                     -- 사용자 FK
  provider_id       CHAR(36)        NOT NULL,                     -- 제공자 FK
  provider_subject  VARCHAR(255)    NOT NULL,                     -- 제공자 사용자 키
  access_token      VARCHAR(1024)   NULL,                         -- 액세스 토큰
  refresh_token     VARCHAR(1024)   NULL,                         -- 리프레시 토큰
  token_expires_at  DATETIME        NULL,                         -- 토큰 만료 일시
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
  UNIQUE KEY uq_user_identities_provider_subject (provider_id, provider_subject),
  UNIQUE KEY uq_user_identities_user_provider (user_id, provider_id),
  CONSTRAINT fk_user_identities_user
    FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_identities_provider
    FOREIGN KEY (provider_id) REFERENCES tb_auth_providers(id) ON DELETE RESTRICT
);

-- Sessions (refresh tokens or session tracking)
CREATE TABLE tb_sessions (
  id                  CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 세션 PK
  user_id             CHAR(36)        NOT NULL,                     -- 사용자 FK
  login_provider      VARCHAR(20)     NOT NULL,                     -- 로그인 제공자
  access_token_hash   VARCHAR(255)    NOT NULL,                     -- 액세스 토큰 해시
  refresh_token_hash  VARCHAR(255)    NOT NULL,                     -- 리프레시 토큰 해시
  access_expires_at   DATETIME        NOT NULL,                     -- 액세스 토큰 만료 일시
  refresh_expires_at  DATETIME        NOT NULL,                     -- 리프레시 토큰 만료 일시
  ip_address          VARCHAR(45)     NULL,                         -- IP 주소
  user_agent          VARCHAR(255)    NULL,                         -- User-Agent
  device_id           VARCHAR(100)    NULL,                         -- 디바이스 식별자
  device_name         VARCHAR(100)    NULL,                         -- 디바이스 이름
  issued_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 발급 일시
  last_seen_at        DATETIME        NULL,                         -- 최근 접속 일시
  revoked_at          DATETIME        NULL,                         -- 폐기 일시
  revoked_reason      VARCHAR(100)    NULL,                         -- 폐기 사유
  CONSTRAINT fk_sessions_user
    FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE,
  KEY idx_sessions_user (user_id),
  KEY idx_sessions_access_expires_at (access_expires_at),
  KEY idx_sessions_refresh_expires_at (refresh_expires_at)
);

-- Groups
CREATE TABLE tb_groups (
  id             CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 그룹 PK
  group_code     VARCHAR(50)     NOT NULL,                     -- 그룹 코드 (프로그래밍 식별자)
  name           VARCHAR(100)    NOT NULL,                     -- 그룹 표시명
  description    VARCHAR(255)    NULL,                         -- 그룹 설명
  is_system      TINYINT(1)      NOT NULL DEFAULT 0,           -- 시스템 그룹 여부 (삭제/코드변경 불가)
  owner_user_id  CHAR(36)        NULL,                         -- 그룹 소유자 (시스템 그룹은 NULL)
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
  updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  CONSTRAINT fk_groups_owner
    FOREIGN KEY (owner_user_id) REFERENCES tb_users(id) ON DELETE RESTRICT,
  UNIQUE KEY uq_groups_group_code (group_code),
  UNIQUE KEY uq_groups_name (name)
);

-- Group members (tb_roles)
CREATE TABLE tb_group_members (
  group_id  CHAR(36)    NOT NULL,                     -- 그룹 FK
  user_id   CHAR(36)    NOT NULL,                     -- 사용자 FK
  joined_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 가입 일시
  PRIMARY KEY (group_id, user_id),
  CONSTRAINT fk_group_members_group
    FOREIGN KEY (group_id) REFERENCES tb_groups(id) ON DELETE CASCADE,
  CONSTRAINT fk_group_members_user
    FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE
);

-- Optional: group invites
CREATE TABLE tb_group_invites (
  id              CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 초대 PK
  group_id        CHAR(36)        NOT NULL,                     -- 그룹 FK
  email           VARCHAR(255)    NOT NULL,                     -- 초대 이메일
  inviter_user_id CHAR(36)        NOT NULL,                     -- 초대한 사용자 FK
  token           CHAR(64)        NOT NULL,                     -- 초대 토큰
  expires_at      DATETIME        NOT NULL,                     -- 만료 일시
  accepted_at     DATETIME        NULL,                         -- 수락 일시
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
  UNIQUE KEY uq_group_invites_token (token),
  CONSTRAINT fk_group_invites_group
    FOREIGN KEY (group_id) REFERENCES tb_groups(id) ON DELETE CASCADE,
  CONSTRAINT fk_group_invites_inviter
    FOREIGN KEY (inviter_user_id) REFERENCES tb_users(id) ON DELETE CASCADE
);

-- RBAC: tb_roles, tb_permissions, tb_resources (module-level)
CREATE TABLE tb_resources (
  id         CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 리소스 PK
  code       VARCHAR(50)     NOT NULL,                     -- 리소스 코드
  name       VARCHAR(100)    NOT NULL,                     -- 리소스 이름
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
  UNIQUE KEY uq_resources_code (code)
);

CREATE TABLE tb_permissions (
  id          CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 권한 PK
  resource_id CHAR(36)        NOT NULL,                     -- 리소스 FK
  action      VARCHAR(30)     NOT NULL,                     -- 권한 액션
  name        VARCHAR(100)    NOT NULL,                     -- 권한 이름
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
  UNIQUE KEY uq_permissions_resource_action (resource_id, action),
  CONSTRAINT fk_permissions_resource
    FOREIGN KEY (resource_id) REFERENCES tb_resources(id) ON DELETE CASCADE
);

-- 통합 설정 (시스템 + 모듈 설정을 하나의 테이블로 관리)
-- module_code='system'이면 전역 설정, 그 외는 해당 모듈의 설정
-- setting_group으로 설정을 그룹핑 (general/signup/auth/session 등)
CREATE TABLE tb_settings (
  id              CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                  -- 설정 PK
  module_code     VARCHAR(50)     NOT NULL DEFAULT 'system',                                     -- 'system' = 전역, 그 외 = 모듈 코드
  setting_group   VARCHAR(50)     NOT NULL DEFAULT 'general',                                    -- 설정 그룹 ('general' = 기본)
  setting_key     VARCHAR(100)    NOT NULL,                                                      -- 설정 키
  setting_value   TEXT            NOT NULL,                                                       -- 설정 값 (문자열 저장)
  value_type      VARCHAR(20)     NOT NULL DEFAULT 'STRING',                                     -- STRING/NUMBER/BOOLEAN/JSON
  name            VARCHAR(100)    NOT NULL,                                                      -- 표시명 (관리자 UI)
  description     VARCHAR(500)    NULL,                                                          -- 설명
  is_readonly     TINYINT(1)      NOT NULL DEFAULT 0,                                            -- 읽기 전용 (코드에서만 변경)
  sort_order      INT             NOT NULL DEFAULT 0,                                            -- 그룹 내 정렬
  updated_by      VARCHAR(100)    NULL,                                                          -- 최종 수정자
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 생성 일시
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  UNIQUE KEY uq_settings (module_code, setting_group, setting_key),
  KEY idx_settings_module (module_code)
);

-- Modules (single or multi-instance)
CREATE TABLE tb_modules (
  id          CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                  -- 모듈 PK
  code        VARCHAR(50)     NOT NULL,                                                      -- 모듈 코드 (영소문자, 프로그래밍 식별자)
  name        VARCHAR(100)    NOT NULL,                                                      -- 모듈 표시명
  slug        VARCHAR(50)     NOT NULL,                                                      -- URL 슬러그 (영소문자+숫자+하이픈)
  description VARCHAR(500)    NULL,                                                          -- 모듈 설명
  type        VARCHAR(20)     NOT NULL DEFAULT 'SINGLE',                                     -- 모듈 유형 (SINGLE / MULTI)
  is_enabled  TINYINT(1)      NOT NULL DEFAULT 1,                                            -- 활성화 여부
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 생성 일시
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  UNIQUE KEY uq_modules_code (code),
  UNIQUE KEY uq_modules_slug (slug)
);

-- Module instances (for multi-instance tb_modules like boards)
CREATE TABLE tb_module_instances (
  instance_id     VARCHAR(50)     PRIMARY KEY DEFAULT (UUID()),                                  -- 인스턴스 PK (UUID)
  module_code     VARCHAR(50)     NOT NULL,                                                      -- 모듈 코드 FK
  instance_name   VARCHAR(100)    NOT NULL,                                                      -- 인스턴스 표시명
  slug            VARCHAR(50)     NOT NULL,                                                      -- URL 슬러그 (동일 모듈 내 유니크)
  description     VARCHAR(500)    NULL,                                                          -- 인스턴스 설명
  owner_id        VARCHAR(50)     NOT NULL,                                                      -- 소유자 사용자 PK
  instance_type   VARCHAR(20)     NOT NULL,                                                      -- 인스턴스 유형
  enabled         TINYINT(1)      NOT NULL DEFAULT 1,                                            -- 활성화 여부
  created_by      VARCHAR(100)    NOT NULL,                                                      -- 생성자
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 생성 일시
  updated_by      VARCHAR(100)    NULL,                                                          -- 수정자
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  CONSTRAINT fk_module_instances_module
    FOREIGN KEY (module_code) REFERENCES tb_modules(code) ON DELETE RESTRICT,
  UNIQUE KEY uq_module_instances_module_name (module_code, instance_name),
  UNIQUE KEY uq_module_instances_module_slug (module_code, slug),
  KEY idx_module_instances_owner_id (owner_id),
  KEY idx_module_instances_enabled (enabled)
);

-- Permissions per module (3-level: module → resource → action)
CREATE TABLE tb_module_permissions (
  id          CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                  -- 모듈 권한 PK
  module_code VARCHAR(50)     NOT NULL,                                                      -- 모듈 코드 FK
  resource    VARCHAR(50)     NOT NULL,                                                      -- 모듈 내 리소스 (post, comment 등)
  action      VARCHAR(30)     NOT NULL,                                                      -- 권한 액션 (read, write, delete 등)
  name        VARCHAR(100)    NOT NULL,                                                      -- 권한 표시명 (게시글 작성 등)
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 생성 일시
  UNIQUE KEY uq_module_permissions (module_code, resource, action),
  CONSTRAINT fk_module_permissions_module
    FOREIGN KEY (module_code) REFERENCES tb_modules(code) ON DELETE RESTRICT
);

-- Direct user tb_permissions for a module instance
CREATE TABLE tb_user_module_permissions (
  user_id              CHAR(36)    NOT NULL,                     -- 사용자 FK
  module_instance_id   VARCHAR(50) NOT NULL,                     -- 모듈 인스턴스 FK
  module_permission_id CHAR(36)    NOT NULL,                     -- 모듈 권한 FK
  granted_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 부여 일시
  PRIMARY KEY (user_id, module_instance_id, module_permission_id),
  CONSTRAINT fk_user_module_permissions_user
    FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_module_permissions_instance
    FOREIGN KEY (module_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE CASCADE,
  CONSTRAINT fk_user_module_permissions_permission
    FOREIGN KEY (module_permission_id) REFERENCES tb_module_permissions(id) ON DELETE CASCADE
);

-- Direct group tb_permissions for a module instance
CREATE TABLE tb_group_module_permissions (
  group_id             CHAR(36)    NOT NULL,                     -- 그룹 FK
  module_instance_id   VARCHAR(50) NOT NULL,                     -- 모듈 인스턴스 FK
  module_permission_id CHAR(36)    NOT NULL,                     -- 모듈 권한 FK
  granted_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 부여 일시
  PRIMARY KEY (group_id, module_instance_id, module_permission_id),
  CONSTRAINT fk_group_module_permissions_group
    FOREIGN KEY (group_id) REFERENCES tb_groups(id) ON DELETE CASCADE,
  CONSTRAINT fk_group_module_permissions_instance
    FOREIGN KEY (module_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE CASCADE,
  CONSTRAINT fk_group_module_permissions_permission
    FOREIGN KEY (module_permission_id) REFERENCES tb_module_permissions(id) ON DELETE CASCADE
);

-- Navigation menus (tree structure)
CREATE TABLE tb_menus (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),   -- 메뉴 PK
  parent_id             CHAR(36)        NULL,                           -- 부모 메뉴 FK (NULL이면 최상위)
  name                  VARCHAR(100)    NOT NULL,                       -- 메뉴 표시명
  icon                  VARCHAR(50)     NULL,                           -- 아이콘 식별자
  menu_type             VARCHAR(20)     NOT NULL DEFAULT 'MODULE',      -- 메뉴 유형 (MODULE/LINK/SEPARATOR)
  module_instance_id    VARCHAR(50)     NULL,                           -- 연결된 모듈 인스턴스 FK (MODULE 타입)
  custom_url            VARCHAR(500)    NULL,                           -- 커스텀 링크 URL (LINK 타입)
  alias_path            VARCHAR(100)    NULL,                           -- 외부 단축 경로 (예: "test", "free")
  content_path          VARCHAR(200)    NULL,                           -- SINGLE 모듈 콘텐츠 경로 (예: "test" → /page/test)
  sort_order            INT             NOT NULL DEFAULT 0,             -- 정렬 순서
  is_visible            TINYINT(1)      NOT NULL DEFAULT 1,             -- 관리자 수동 노출 제어
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 생성 일시
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  CONSTRAINT fk_menus_parent
    FOREIGN KEY (parent_id) REFERENCES tb_menus(id) ON DELETE CASCADE,
  CONSTRAINT fk_menus_module_instance
    FOREIGN KEY (module_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE SET NULL,
  KEY idx_menus_parent_id (parent_id),
  KEY idx_menus_sort_order (sort_order),
  UNIQUE KEY uq_menus_alias_path (alias_path)
);

-- Page module: pages (text/HTML content)
CREATE TABLE tb_page_pages (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),   -- 페이지 PK
  slug                  VARCHAR(100)    NOT NULL,                       -- URL slug (유니크)
  title                 VARCHAR(200)    NOT NULL,                       -- 페이지 제목
  content               LONGTEXT        NULL,                           -- 페이지 본문
  content_type          VARCHAR(20)     NOT NULL DEFAULT 'HTML',        -- 콘텐츠 유형 (HTML/MARKDOWN/TEXT)
  is_published          TINYINT(1)      NOT NULL DEFAULT 0,             -- 공개 여부
  sort_order            INT             NOT NULL DEFAULT 0,             -- 정렬 순서
  module_instance_id    VARCHAR(50)     NULL,                           -- 연결된 모듈 인스턴스 ID (권한 관리용)
  created_by            VARCHAR(100)    NOT NULL,                       -- 작성자
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 생성 일시
  updated_by            VARCHAR(100)    NULL,                           -- 수정자
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  UNIQUE KEY uq_page_pages_slug (slug),
  CONSTRAINT fk_page_pages_module_instance
    FOREIGN KEY (module_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE SET NULL
);

CREATE TABLE tb_roles (
  id         CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 역할 PK
  code       VARCHAR(50)     NOT NULL,                     -- 역할 코드
  name       VARCHAR(100)    NOT NULL,                     -- 역할 이름
  scope      VARCHAR(20)     NOT NULL DEFAULT 'GLOBAL',    -- 역할 범위
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
  UNIQUE KEY uq_roles_code (code)
);

CREATE TABLE tb_role_permissions (
  role_id       CHAR(36)    NOT NULL, -- 역할 FK
  permission_id CHAR(36)    NOT NULL, -- 권한 FK
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permissions_role
    FOREIGN KEY (role_id) REFERENCES tb_roles(id) ON DELETE CASCADE,
  CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY (permission_id) REFERENCES tb_permissions(id) ON DELETE CASCADE
);

CREATE TABLE tb_user_roles (
  user_id CHAR(36)    NOT NULL, -- 사용자 FK
  role_id CHAR(36)    NOT NULL, -- 역할 FK
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES tb_roles(id) ON DELETE CASCADE
);

CREATE TABLE tb_group_roles (
  group_id CHAR(36)    NOT NULL, -- 그룹 FK
  role_id  CHAR(36)    NOT NULL, -- 역할 FK
  PRIMARY KEY (group_id, role_id),
  CONSTRAINT fk_group_roles_group
    FOREIGN KEY (group_id) REFERENCES tb_groups(id) ON DELETE CASCADE,
  CONSTRAINT fk_group_roles_role
    FOREIGN KEY (role_id) REFERENCES tb_roles(id) ON DELETE CASCADE
);

CREATE TABLE tb_group_member_roles (
  group_id CHAR(36)    NOT NULL, -- 그룹 FK
  user_id  CHAR(36)    NOT NULL, -- 사용자 FK
  role_id  CHAR(36)    NOT NULL, -- 역할 FK
  PRIMARY KEY (group_id, user_id, role_id),
  CONSTRAINT fk_group_member_roles_member
    FOREIGN KEY (group_id, user_id) REFERENCES tb_group_members(group_id, user_id) ON DELETE CASCADE,
  CONSTRAINT fk_group_member_roles_role
    FOREIGN KEY (role_id) REFERENCES tb_roles(id) ON DELETE CASCADE
);

-- SMS 발송 프로바이더 (SOLAPI, AWS SNS 등)
CREATE TABLE tb_sms_providers (
  id              CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                  -- 프로바이더 PK
  code            VARCHAR(50)     NOT NULL,                                                      -- 프로바이더 코드 (solapi, aws_sns)
  name            VARCHAR(100)    NOT NULL,                                                      -- 프로바이더 표시명
  is_enabled      TINYINT(1)      NOT NULL DEFAULT 0,                                            -- 사용 여부
  api_key         VARCHAR(255)    NULL,                                                          -- API Key
  api_secret      VARCHAR(255)    NULL,                                                          -- API Secret
  sender_number   VARCHAR(20)     NULL,                                                          -- 발신 번호
  config_json     TEXT            NULL,                                                          -- 추가 설정 JSON
  display_order   INT             NOT NULL DEFAULT 0,                                            -- 표시 순서
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 생성 일시
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  UNIQUE KEY uq_sms_providers_code (code)
);

-- SMS 발송 이력 (감사 로그와 분리 — 수신자/메시지 중심 이력)
CREATE TABLE tb_sms_logs (
  id                CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                  -- SMS 로그 PK
  send_type         VARCHAR(20)     NOT NULL,                                                      -- 발송 유형 (MANUAL/AUTO)
  trigger_type      VARCHAR(50)     NULL,                                                          -- AUTO 전용: 트리거 유형 (PASSWORD_RESET 등)
  sender_user_id    CHAR(36)        NULL,                                                          -- 발송자(관리자) PK
  recipient_phone   VARCHAR(20)     NOT NULL,                                                      -- 수신 전화번호
  recipient_user_id CHAR(36)        NULL,                                                          -- 수신자 PK
  message           TEXT            NOT NULL,                                                      -- 메시지 내용
  provider_code     VARCHAR(50)     NULL,                                                          -- 사용 프로바이더 코드
  send_status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING',                                    -- 발송 상태 (PENDING/SUCCESS/FAILED)
  error_message     VARCHAR(500)    NULL,                                                          -- 실패 시 에러 메시지
  batch_id          CHAR(36)        NULL,                                                          -- 대량 발송 묶음 ID
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 발송 일시
  KEY idx_sms_logs_batch_id (batch_id),
  KEY idx_sms_logs_created_at (created_at),
  KEY idx_sms_logs_send_type (send_type)
);

-- Audit logs
CREATE TABLE tb_audit_logs (
  id             CHAR(36)        PRIMARY KEY DEFAULT (UUID()), -- 감사 로그 PK
  actor_user_id  CHAR(36)        NULL,                         -- 행위자 사용자 FK
  actor_type     VARCHAR(20)     NOT NULL,                     -- 행위자 유형 (USER/SYSTEM)
  action_type    VARCHAR(50)     NOT NULL,                     -- 액션 유형
  target_type    VARCHAR(50)     NULL,                         -- 대상 유형
  target_id      CHAR(36)        NULL,                         -- 대상 ID
  result_status  VARCHAR(20)     NOT NULL,                     -- 결과 상태
  message        VARCHAR(500)    NULL,                         -- 요약 메시지
  metadata_json  JSON            NULL,                         -- 상세 메타데이터
  ip_address     VARCHAR(45)     NULL,                         -- IP 주소
  user_agent     VARCHAR(255)    NULL,                         -- User-Agent
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 발생 일시
  CONSTRAINT fk_audit_logs_actor_user
    FOREIGN KEY (actor_user_id) REFERENCES tb_users(id) ON DELETE SET NULL,
  KEY idx_audit_logs_actor_user (actor_user_id),
  KEY idx_audit_logs_action_type (action_type),
  KEY idx_audit_logs_created_at (created_at)
);
