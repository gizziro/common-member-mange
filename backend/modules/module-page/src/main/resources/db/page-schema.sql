-- 페이지 모듈 스키마 (ModuleSchemaInitializer에 의해 자동 실행)
-- 테이블이 이미 존재하면 실행되지 않음 (멱등)

CREATE TABLE IF NOT EXISTS tb_page_pages (
  id              CHAR(36)        PRIMARY KEY DEFAULT (UUID()),                                  -- 페이지 PK
  slug            VARCHAR(100)    NOT NULL,                                                      -- URL 슬러그 (유니크)
  title           VARCHAR(200)    NOT NULL,                                                      -- 페이지 제목
  content         LONGTEXT        NULL,                                                          -- 페이지 본문
  content_type    VARCHAR(20)     NOT NULL DEFAULT 'HTML',                                       -- 콘텐츠 유형 (HTML/MARKDOWN/TEXT)
  is_published    TINYINT(1)      NOT NULL DEFAULT 0,                                            -- 공개 여부
  sort_order      INT             NOT NULL DEFAULT 0,                                            -- 정렬 순서
  created_by      VARCHAR(100)    NOT NULL,                                                      -- 생성자
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 생성 일시
  updated_by      VARCHAR(100)    NULL,                                                          -- 수정자
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
  UNIQUE KEY uq_page_pages_slug (slug)
);
