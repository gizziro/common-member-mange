-- 클라이언트 인코딩을 utf8mb4로 설정 (한국어 깨짐 방지)
SET NAMES utf8mb4;

-- Seed data (optional)
INSERT INTO tb_auth_providers (code, name) VALUES
  ('local', 'Local'),
  ('google', 'Google'),
  ('github', 'GitHub');

-- 시스템 기본 그룹
INSERT INTO tb_groups (group_code, name, description, is_system) VALUES
  ('administrator', '관리자',   '시스템 관리자 그룹', 1),
  ('user',          '일반회원', '기본 회원 그룹',     1);
