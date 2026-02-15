# 프로젝트 환경

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 (Microsoft OpenJDK) |
| Framework | Spring Boot | 4.0.2 |
| Build | Gradle (Groovy DSL) | 9.3.0 |
| DB | MySQL | 8.0 |
| Cache (예정) | Redis | - |
| Frontend | Next.js + TypeScript + Tailwind CSS | latest |
| Container | Docker + Docker Compose | - |

## 프로젝트 구조

```
common-member-mange/
├── backend/                    # Spring Boot 멀티모듈 (Gradle 루트)
│   ├── buildSrc/               #   컨벤션 플러그인 (공통 빌드 설정)
│   ├── core/                   #   공통 라이브러리 (도메인/서비스/모듈 프레임워크/예외처리)
│   ├── modules/                #   기능 모듈 (각각 독립 Gradle 프로젝트)
│   │   ├── module-board/       #     게시판 모듈
│   │   ├── module-blog/        #     블로그 모듈
│   │   └── module-accounting/  #     가계부 모듈
│   ├── admin-api/              #   관리자 API (포트 5000)
│   └── user-api/               #   사용자 API (포트 6100)
├── frontend/
│   ├── admin/                  # 관리자 웹 (Next.js)
│   └── user/                   # 사용자 웹 (Next.js)
├── db/
│   └── init/                   # DB 초기화 스크립트 (schema.sql, data.sql)
├── docker-compose.yml          # 컨테이너 오케스트레이션
├── CLAUDE.md                   # Claude Code 지침 (메인)
├── INFO.MD                     # 인증/인가 정책 문서
└── NOTICE.MD                   # 구현 체크리스트
```

## 모듈 의존관계

```
admin-api ──→ core
          ──→ modules/module-board      (선택)
          ──→ modules/module-blog       (선택)

user-api  ──→ core
          ──→ modules/module-board      (선택)
          ──→ modules/module-blog       (선택)

module-board ──→ core
module-blog  ──→ core
```

- `core`: 라이브러리 모듈 (실행 불가, 모듈 프레임워크 + 공통 기능)
- `modules/*`: 기능 모듈 (core 참조, `ModuleDefinition` 인터페이스 구현)
- `admin-api`, `user-api`: 실행 가능한 Spring Boot 애플리케이션, 필요한 기능 모듈을 dependency로 선택
- 기능 모듈 내 컨트롤러는 `@ConditionalOnProperty(name = "app.api-type")` 로 admin/user 선택적 활성화

## 빌드 & 실행 명령어

모든 Gradle 명령어는 `backend/` 디렉토리에서 실행:

```bash
# 빌드
./gradlew clean build              # 전체 빌드 (core + admin-api + user-api)
./gradlew :core:build              # core 모듈만 빌드
./gradlew :admin-api:bootJar       # 관리자 API JAR 빌드
./gradlew :user-api:bootJar        # 사용자 API JAR 빌드

# 로컬 실행
./gradlew :admin-api:bootRun       # 관리자 API (localhost:5000)
./gradlew :user-api:bootRun        # 사용자 API (localhost:6100)

# 테스트
./gradlew test                     # 전체 테스트
./gradlew :core:test               # core 모듈 테스트만
```

## Docker 명령어

```bash
# 프로필 기반 실행
docker compose --profile full up --build     # 전체 (admin + user + db)
docker compose --profile admin up --build    # 관리자 API + db
docker compose --profile user up --build     # 사용자 API + db
docker compose up -d db                      # MySQL만 (로컬 개발용)

# 종료 및 초기화
docker compose --profile full down           # 컨테이너 종료
docker compose --profile full down -v        # DB 초기화 (볼륨 삭제 후 init SQL 재실행)
```

## Frontend 명령어

```bash
cd frontend/admin && npm run dev   # 관리자 프론트엔드 개발 서버
cd frontend/user && npm run dev    # 사용자 프론트엔드 개발 서버
```

## 로컬 개발 환경

| 서비스 | 주소 | 비고 |
|--------|------|------|
| Admin API | `localhost:5000` | 관리자 전용 |
| User API | `localhost:6100` | 일반 사용자 |
| MySQL | `localhost:13306` | 외부 포트 매핑 |
| DB Name | `app_db` | |
| DB User | `app_user` / `app_password` | |

## 주요 참고 문서

| 파일 | 설명 |
|------|------|
| `INFO.MD` | 인증/인가/모듈/권한 정책 전체 문서 |
| `NOTICE.MD` | 구현해야 할 기능 체크리스트 |
| `AGENTS.md` | 코딩 컨벤션 및 프로젝트 구조 가이드라인 |
| `db/init/schema.sql` | 데이터베이스 스키마 정의 (23개 테이블) |
