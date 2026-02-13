# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0.2 member management system with RBAC, group/module permissions, and OAuth2 support. Java 21, Gradle build, MySQL 8.0 via Docker. Currently in early development — schema and policies are defined but most application code is yet to be implemented.

## Build & Run Commands

All Gradle commands run from `backend/`:

```bash
./gradlew clean build              # 전체 빌드 (core + admin-api + user-api)
./gradlew :core:build              # core 모듈만 빌드
./gradlew :admin-api:bootRun       # 관리자 API 로컬 실행 (포트 5000)
./gradlew :user-api:bootRun        # 사용자 API 로컬 실행 (포트 6000)
./gradlew :admin-api:bootJar       # 관리자 API JAR 빌드
./gradlew :user-api:bootJar        # 사용자 API JAR 빌드
./gradlew test                     # 전체 테스트
```

Docker (profiles 기반):
```bash
docker compose --profile full up --build    # 전체 (admin-api + user-api + db)
docker compose --profile admin up --build   # 관리자 API + db
docker compose --profile user up --build    # 사용자 API + db
docker compose up -d db                     # MySQL only (IntelliJ 로컬 개발용)
docker compose --profile full down -v       # DB 초기화 (볼륨 삭제, init SQL 재실행)
```

Frontend (각 디렉토리에서 실행):
```bash
cd frontend/admin && npm run dev   # 관리자 프론트엔드 (Next.js)
cd frontend/user && npm run dev    # 사용자 프론트엔드 (Next.js)
```

## Local Development

- Admin API: `localhost:5000`
- User API: `localhost:6000`
- MySQL: `localhost:13306`, database `app_db`, user `app_user` / `app_password`
- DB init scripts (`db/init/schema.sql`, `db/init/data.sql`) run only on first container creation. To re-initialize, destroy the volume with `docker compose --profile full down -v`.

## Code Conventions

- **모든 주석은 한국어로 작성** — 코드 한 줄에 주석 한 줄 수준으로 세세하게 작성
- Java conventions: 4-space indent, `UpperCamelCase` classes, `lowerCamelCase` methods/fields, `UPPER_SNAKE_CASE` constants
- Package root: `com.gizzi` (core: `com.gizzi.core`, admin: `com.gizzi.admin`, user: `com.gizzi.user`)
- One public class per file
- **Column-aligned formatting** preferred for SQL and declarations:
  ```sql
  id        CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
  email     VARCHAR(255)    NOT NULL,
  ```
- Lombok is available (`compileOnly`). Use it to reduce boilerplate.
- No formatter/linter configured — match existing style.

## Architecture

### Database Schema (23 tables, `tb_` prefix)

The schema in `db/init/schema.sql` defines the full data model:

- **User & Identity**: `tb_users`, `tb_auth_providers`, `tb_user_identities`, `tb_sessions`
- **Groups**: `tb_groups`, `tb_group_members`, `tb_group_invites`
- **Modules**: `tb_modules` (SINGLE/MULTI type), `tb_module_instances`, `tb_module_permissions`
- **Permission grants**: `tb_user_module_permissions`, `tb_group_module_permissions`
- **Global RBAC**: `tb_resources`, `tb_permissions`, `tb_roles` (GLOBAL/GROUP scope), `tb_role_permissions`, `tb_user_roles`, `tb_group_roles`, `tb_group_member_roles`
- **System**: `tb_system_settings` (feature flags), `tb_audit_logs`

### Permission Check Flow (as defined in INFO.MD)

1. Check user global roles (`tb_user_roles`)
2. Check module instance ownership (`tb_module_instances.owner_id`)
3. Check user direct module permissions (`tb_user_module_permissions`)
4. Aggregate group permissions (`tb_group_module_permissions`)
5. User direct + group permissions are additive

### Authentication Design

- Local login: `user_id` + `password_hash`
- Social login: OAuth2 via `tb_auth_providers` + `tb_user_identities`
- JWT: Access Token + Refresh Token, store in Redis, audit in DB (`tb_sessions`)
- 2FA/OTP support via `is_otp_use` / `otp_secret` on `tb_users`

### Multi-Module Structure

Gradle 멀티모듈 프로젝트 (`backend/` 루트):

- `core` — 공통 도메인/엔티티/리포지토리/서비스/권한/예외처리 (라이브러리, 실행 불가)
- `admin-api` — 관리자 API (포트 5000), core 참조, 개별 Spring Security 설정
- `user-api` — 사용자 API (포트 6000), core 참조, 개별 Spring Security 설정
- `buildSrc` — Gradle 컨벤션 플러그인 (공통 빌드 설정)

Frontend (`frontend/`):

- `frontend/admin` — 관리자 웹 (Next.js, TypeScript, Tailwind)
- `frontend/user` — 사용자 웹 (Next.js, TypeScript, Tailwind)

### Planned API Endpoints

- Auth: `POST /auth/signup`, `/auth/login`, `/auth/refresh`, `/auth/logout`
- Groups: `POST /groups`, `GET /groups/{id}`, `POST /groups/{id}/invites`, `POST /groups/{id}/members`
- Modules: `POST /modules`, `GET /modules`, `POST /modules/{code}/instances`
- Permissions: `POST /permissions/grant`, `POST /permissions/revoke`, `GET /permissions/check`

## Key Documentation

- `INFO.MD` — Full authentication/authorization policy documentation
- `NOTICE.MD` — Implementation checklist for all features to build
- `AGENTS.md` — Coding conventions and project structure guidelines
