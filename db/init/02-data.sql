-- 클라이언트 인코딩을 utf8mb4로 설정 (한국어 깨짐 방지)
SET NAMES utf8mb4;

-- 인증 제공자 시드 데이터
-- local은 활성, 소셜 Provider는 client_id/secret 미설정이므로 비활성 기본
INSERT INTO tb_auth_providers (code, name, is_enabled, authorization_uri, token_uri, userinfo_uri, scope, display_order) VALUES
  ('local',  '로컬',   1, NULL, NULL, NULL, NULL, 0),
  ('google', 'Google', 0,
    'https://accounts.google.com/o/oauth2/v2/auth',
    'https://oauth2.googleapis.com/token',
    'https://www.googleapis.com/oauth2/v3/userinfo',
    'openid email profile',
    1),
  ('kakao',  '카카오', 0,
    'https://kauth.kakao.com/oauth/authorize',
    'https://kauth.kakao.com/oauth/token',
    'https://kapi.kakao.com/v2/user/me',
    'profile_nickname account_email',
    2),
  ('naver',  '네이버', 0,
    'https://nid.naver.com/oauth2.0/authorize',
    'https://nid.naver.com/oauth2.0/token',
    'https://openapi.naver.com/v1/nid/me',
    '',
    3);

-- 시스템 기본 그룹
INSERT INTO tb_groups (group_code, name, description, is_system) VALUES
  ('administrator', '관리자',   '시스템 관리자 그룹', 1),
  ('user',          '일반회원', '기본 회원 그룹',     1);
