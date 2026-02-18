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

-- SMS 프로바이더 시드 데이터
INSERT INTO tb_sms_providers (code, name, is_enabled, display_order) VALUES
  ('solapi',  'SOLAPI',  0, 0),
  ('aws_sns', 'AWS SNS', 0, 1);

-- 시스템 기본 그룹
-- INSERT INTO tb_groups (group_code, name, description, is_system) VALUES
--   ('administrator', '관리자',   '시스템 관리자 그룹', 1),
--   ('user',          '일반회원', '기본 회원 그룹',     1);


INSERT INTO app_db.tb_groups (id, group_code, name, description, is_system, owner_user_id, created_at, updated_at) VALUES ('ce26c18b-0bfb-11f1-89f0-de534346bb47', 'administrator', '관리자', '시스템 관리자 그룹', 1, null, '2026-02-17 12:25:54', '2026-02-17 12:25:54');
INSERT INTO app_db.tb_groups (id, group_code, name, description, is_system, owner_user_id, created_at, updated_at) VALUES ('ce26c7fd-0bfb-11f1-89f0-de534346bb47', 'user', '일반회원', '기본 회원 그룹', 1, null, '2026-02-17 12:25:54', '2026-02-17 12:25:54');

INSERT INTO app_db.tb_users (id, user_id, username, provider, provider_id, password_hash, password_change_date, email, email_token, email_verified, phone, phone_verified, is_sms_agree, social_join_verified, social_join_token, is_otp_use, otp_secret, login_fail_count, is_locked, locked_at, user_status, created_by, created_at, updated_by, updated_at) VALUES ('855982a8-26c7-4a45-9b2b-11bcc156dabd', 'admin', '박기태', 'LOCAL', 'admin', '$2a$10$tnZURMsV2zCu6OwfprR8JOr14FXdyFx/jSRCDIWS.3vRyIpT2mu9e', '2026-02-17 21:27:56', 'gitae@kakao.com', null, 0, null, 0, 0, 0, null, 0, null, 0, 0, null, 'ACTIVE', 'admin', '2026-02-17 21:27:56', null, '2026-02-17 21:27:56');

INSERT INTO app_db.tb_group_members (group_id, user_id, joined_at) VALUES ('ce26c18b-0bfb-11f1-89f0-de534346bb47', '855982a8-26c7-4a45-9b2b-11bcc156dabd', '2026-02-17 21:27:56');
INSERT INTO app_db.tb_group_members (group_id, user_id, joined_at) VALUES ('ce26c7fd-0bfb-11f1-89f0-de534346bb47', '855982a8-26c7-4a45-9b2b-11bcc156dabd', '2026-02-17 21:27:56');
