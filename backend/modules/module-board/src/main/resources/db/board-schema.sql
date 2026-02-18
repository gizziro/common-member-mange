-- 게시판 모듈 스키마 (ModuleSchemaInitializer에 의해 자동 실행)
-- 테이블이 이미 존재하면 실행되지 않음 (멱등)

-- 1. 게시판 설정 (모듈 인스턴스 1:1)
CREATE TABLE IF NOT EXISTS tb_board_settings (
  board_instance_id     VARCHAR(50)     PRIMARY KEY,                                                     -- 게시판 인스턴스 ID (FK → tb_module_instances)
  editor_type           VARCHAR(20)     NOT NULL DEFAULT 'MARKDOWN',                                     -- 에디터 타입 (PLAIN_TEXT/MARKDOWN)
  posts_per_page        INT             NOT NULL DEFAULT 20,                                              -- 페이지당 게시글 수
  display_format        VARCHAR(20)     NOT NULL DEFAULT 'LIST',                                         -- 목록 표시 형식 (LIST/GALLERY/CARD)
  pagination_type       VARCHAR(20)     NOT NULL DEFAULT 'OFFSET',                                       -- 페이지네이션 방식 (OFFSET/CURSOR)
  allow_anonymous_access TINYINT(1)     NOT NULL DEFAULT 0,                                              -- 비로그인 사용자 접근 허용 여부
  allow_file_upload     TINYINT(1)      NOT NULL DEFAULT 1,                                              -- 파일 업로드 허용 여부
  allowed_file_types    VARCHAR(500)    NULL DEFAULT 'jpg,jpeg,png,gif,pdf,zip,doc,docx,xls,xlsx',       -- 허용 파일 확장자 (쉼표 구분)
  max_file_size         BIGINT          NOT NULL DEFAULT 10485760,                                       -- 최대 파일 크기 (바이트, 기본 10MB)
  max_files_per_post    INT             NOT NULL DEFAULT 5,                                              -- 게시글당 최대 파일 수
  max_reply_depth       INT             NOT NULL DEFAULT 0,                                              -- 최대 답글 깊이 (0=답글 불가)
  max_comment_depth     INT             NOT NULL DEFAULT 2,                                              -- 최대 댓글 깊이
  allow_secret_posts    TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 비밀글 허용 여부
  allow_draft           TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 임시저장 허용 여부
  allow_tags            TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 태그 사용 여부
  allow_vote            TINYINT(1)      NOT NULL DEFAULT 1,                                              -- 추천/비추천 허용 여부
  use_category          TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 카테고리 사용 여부
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                               -- 생성 일시
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,   -- 수정 일시
  CONSTRAINT fk_board_settings_instance
    FOREIGN KEY (board_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE CASCADE
);

-- 2. 게시판 카테고리
CREATE TABLE IF NOT EXISTS tb_board_categories (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                     -- 카테고리 PK
  board_instance_id     VARCHAR(50)     NOT NULL,                                                        -- 게시판 인스턴스 ID
  name                  VARCHAR(100)    NOT NULL,                                                        -- 카테고리 표시명
  slug                  VARCHAR(50)     NOT NULL,                                                        -- URL 슬러그
  description           VARCHAR(500)    NULL,                                                            -- 카테고리 설명
  sort_order            INT             NOT NULL DEFAULT 0,                                              -- 정렬 순서
  is_active             TINYINT(1)      NOT NULL DEFAULT 1,                                              -- 활성화 여부
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                               -- 생성 일시
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,   -- 수정 일시
  UNIQUE KEY uq_board_categories_slug (board_instance_id, slug),
  CONSTRAINT fk_board_categories_instance
    FOREIGN KEY (board_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE CASCADE
);

-- 3. 게시판 태그
CREATE TABLE IF NOT EXISTS tb_board_tags (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                     -- 태그 PK
  board_instance_id     VARCHAR(50)     NOT NULL,                                                        -- 게시판 인스턴스 ID
  name                  VARCHAR(50)     NOT NULL,                                                        -- 태그명
  slug                  VARCHAR(50)     NOT NULL,                                                        -- URL 슬러그
  post_count            INT             NOT NULL DEFAULT 0,                                              -- 사용 게시글 수 (비정규화)
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                               -- 생성 일시
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,   -- 수정 일시
  UNIQUE KEY uq_board_tags_slug (board_instance_id, slug),
  CONSTRAINT fk_board_tags_instance
    FOREIGN KEY (board_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE CASCADE
);

-- 4. 게시글
CREATE TABLE IF NOT EXISTS tb_board_posts (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                     -- 게시글 PK
  board_instance_id     VARCHAR(50)     NOT NULL,                                                        -- 게시판 인스턴스 ID
  category_id           CHAR(36)        NULL,                                                            -- 카테고리 FK (선택)
  parent_id             CHAR(36)        NULL,                                                            -- 부모 게시글 FK (답글 시)
  depth                 INT             NOT NULL DEFAULT 0,                                              -- 답글 깊이 (0=원글)
  title                 VARCHAR(300)    NOT NULL,                                                        -- 게시글 제목
  content               LONGTEXT        NULL,                                                            -- 게시글 본문
  content_type          VARCHAR(20)     NOT NULL DEFAULT 'MARKDOWN',                                     -- 콘텐츠 유형 (PLAIN_TEXT/MARKDOWN)
  slug                  VARCHAR(100)    NULL,                                                            -- SEO 슬러그 (선택)
  is_secret             TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 비밀글 여부
  is_notice             TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 공지글 여부
  notice_scope          VARCHAR(20)     NULL,                                                            -- 공지 범위 (BOARD/GLOBAL)
  is_draft              TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 임시저장 여부
  author_id             CHAR(36)        NOT NULL,                                                        -- 작성자 PK
  author_name           VARCHAR(100)    NOT NULL,                                                        -- 작성자명 (비정규화, 탈퇴 대비)
  view_count            INT             NOT NULL DEFAULT 0,                                              -- 조회수
  vote_up_count         INT             NOT NULL DEFAULT 0,                                              -- 추천수
  vote_down_count       INT             NOT NULL DEFAULT 0,                                              -- 비추천수
  comment_count         INT             NOT NULL DEFAULT 0,                                              -- 댓글수
  meta_title            VARCHAR(200)    NULL,                                                            -- SEO 메타 타이틀
  meta_description      VARCHAR(500)    NULL,                                                            -- SEO 메타 설명
  is_deleted            TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 삭제 여부 (소프트 삭제)
  deleted_at            DATETIME        NULL,                                                            -- 삭제 일시
  deleted_by            CHAR(36)        NULL,                                                            -- 삭제자 PK
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                               -- 생성 일시
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,   -- 수정 일시
  KEY idx_board_posts_instance (board_instance_id),
  KEY idx_board_posts_category (category_id),
  KEY idx_board_posts_author (author_id),
  KEY idx_board_posts_created (board_instance_id, is_deleted, created_at DESC),
  KEY idx_board_posts_notice (board_instance_id, is_notice, notice_scope),
  CONSTRAINT fk_board_posts_instance
    FOREIGN KEY (board_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE CASCADE,
  CONSTRAINT fk_board_posts_category
    FOREIGN KEY (category_id) REFERENCES tb_board_categories(id) ON DELETE SET NULL,
  CONSTRAINT fk_board_posts_parent
    FOREIGN KEY (parent_id) REFERENCES tb_board_posts(id) ON DELETE SET NULL,
  CONSTRAINT fk_board_posts_author
    FOREIGN KEY (author_id) REFERENCES tb_users(id) ON DELETE CASCADE
);

-- 5. 게시글 Closure Table (계층형 답글 구현)
CREATE TABLE IF NOT EXISTS tb_board_post_closure (
  ancestor_id           CHAR(36)        NOT NULL,                                                        -- 조상 게시글 PK
  descendant_id         CHAR(36)        NOT NULL,                                                        -- 자손 게시글 PK
  depth                 INT             NOT NULL DEFAULT 0,                                              -- 조상-자손 간 깊이
  PRIMARY KEY (ancestor_id, descendant_id),
  CONSTRAINT fk_board_post_closure_ancestor
    FOREIGN KEY (ancestor_id) REFERENCES tb_board_posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_board_post_closure_descendant
    FOREIGN KEY (descendant_id) REFERENCES tb_board_posts(id) ON DELETE CASCADE
);

-- 6. 게시글-태그 연결 테이블
CREATE TABLE IF NOT EXISTS tb_board_post_tags (
  post_id               CHAR(36)        NOT NULL,                                                        -- 게시글 PK
  tag_id                CHAR(36)        NOT NULL,                                                        -- 태그 PK
  PRIMARY KEY (post_id, tag_id),
  CONSTRAINT fk_board_post_tags_post
    FOREIGN KEY (post_id) REFERENCES tb_board_posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_board_post_tags_tag
    FOREIGN KEY (tag_id) REFERENCES tb_board_tags(id) ON DELETE CASCADE
);

-- 7. 댓글
CREATE TABLE IF NOT EXISTS tb_board_comments (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                     -- 댓글 PK
  post_id               CHAR(36)        NOT NULL,                                                        -- 게시글 FK
  parent_id             CHAR(36)        NULL,                                                            -- 부모 댓글 FK (대댓글 시)
  depth                 INT             NOT NULL DEFAULT 0,                                              -- 댓글 깊이 (0=원댓글)
  content               TEXT            NOT NULL,                                                        -- 댓글 본문
  author_id             CHAR(36)        NOT NULL,                                                        -- 작성자 PK
  author_name           VARCHAR(100)    NOT NULL,                                                        -- 작성자명 (비정규화)
  vote_up_count         INT             NOT NULL DEFAULT 0,                                              -- 추천수
  vote_down_count       INT             NOT NULL DEFAULT 0,                                              -- 비추천수
  is_deleted            TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 삭제 여부 (소프트 삭제)
  deleted_at            DATETIME        NULL,                                                            -- 삭제 일시
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                               -- 생성 일시
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,   -- 수정 일시
  KEY idx_board_comments_post (post_id),
  KEY idx_board_comments_author (author_id),
  CONSTRAINT fk_board_comments_post
    FOREIGN KEY (post_id) REFERENCES tb_board_posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_board_comments_parent
    FOREIGN KEY (parent_id) REFERENCES tb_board_comments(id) ON DELETE SET NULL,
  CONSTRAINT fk_board_comments_author
    FOREIGN KEY (author_id) REFERENCES tb_users(id) ON DELETE CASCADE
);

-- 8. 댓글 Closure Table (계층형 대댓글 구현)
CREATE TABLE IF NOT EXISTS tb_board_comment_closure (
  ancestor_id           CHAR(36)        NOT NULL,                                                        -- 조상 댓글 PK
  descendant_id         CHAR(36)        NOT NULL,                                                        -- 자손 댓글 PK
  depth                 INT             NOT NULL DEFAULT 0,                                              -- 조상-자손 간 깊이
  PRIMARY KEY (ancestor_id, descendant_id),
  CONSTRAINT fk_board_comment_closure_ancestor
    FOREIGN KEY (ancestor_id) REFERENCES tb_board_comments(id) ON DELETE CASCADE,
  CONSTRAINT fk_board_comment_closure_descendant
    FOREIGN KEY (descendant_id) REFERENCES tb_board_comments(id) ON DELETE CASCADE
);

-- 9. 첨부 파일
CREATE TABLE IF NOT EXISTS tb_board_files (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                     -- 파일 PK
  post_id               CHAR(36)        NOT NULL,                                                        -- 게시글 FK
  original_name         VARCHAR(255)    NOT NULL,                                                        -- 원본 파일명
  stored_name           VARCHAR(255)    NOT NULL,                                                        -- 저장 파일명 (UUID 기반)
  file_path             VARCHAR(500)    NOT NULL,                                                        -- 저장 경로
  file_size             BIGINT          NOT NULL,                                                        -- 파일 크기 (바이트)
  mime_type             VARCHAR(100)    NOT NULL,                                                        -- MIME 타입
  is_image              TINYINT(1)      NOT NULL DEFAULT 0,                                              -- 이미지 여부
  thumbnail_path        VARCHAR(500)    NULL,                                                            -- 썸네일 경로 (이미지인 경우)
  sort_order            INT             NOT NULL DEFAULT 0,                                              -- 정렬 순서
  download_count        INT             NOT NULL DEFAULT 0,                                              -- 다운로드 횟수
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                               -- 생성 일시
  KEY idx_board_files_post (post_id),
  CONSTRAINT fk_board_files_post
    FOREIGN KEY (post_id) REFERENCES tb_board_posts(id) ON DELETE CASCADE
);

-- 10. 추천/비추천 (게시글 + 댓글 공용)
CREATE TABLE IF NOT EXISTS tb_board_votes (
  id                    CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                     -- 투표 PK
  target_type           VARCHAR(20)     NOT NULL,                                                        -- 대상 유형 (POST/COMMENT)
  target_id             CHAR(36)        NOT NULL,                                                        -- 대상 PK
  user_id               CHAR(36)        NOT NULL,                                                        -- 투표자 PK
  vote_type             VARCHAR(10)     NOT NULL,                                                        -- 투표 유형 (UP/DOWN)
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                               -- 생성 일시
  UNIQUE KEY uq_board_votes_target_user (target_type, target_id, user_id),
  KEY idx_board_votes_target (target_type, target_id),
  CONSTRAINT fk_board_votes_user
    FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE
);

-- 11. 게시판 부관리자
CREATE TABLE IF NOT EXISTS tb_board_admins (
  board_instance_id     VARCHAR(50)     NOT NULL,                                                        -- 게시판 인스턴스 ID
  user_id               CHAR(36)        NOT NULL,                                                        -- 사용자 PK
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                               -- 생성 일시
  PRIMARY KEY (board_instance_id, user_id),
  CONSTRAINT fk_board_admins_instance
    FOREIGN KEY (board_instance_id) REFERENCES tb_module_instances(instance_id) ON DELETE CASCADE,
  CONSTRAINT fk_board_admins_user
    FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE
);
