# Repository Guidelines

# 주석 정보
- 주석은 모두 한국어로 작성해줘
- 나는 개발에 진심인 개발자로서 주석을 세세하게 작성하는 편이야, 코드 한줄에 주석 한줄을 작성하는 편이니깐 참고해줘

## Project Structure & Module Organization
- `app/` contains the Spring Boot service.
- `app/src/main/java/com/gizzi/web/` holds application source code (entry point: `WebServiceApplication`).
- `app/src/main/resources/` contains configuration such as `application.properties`.
- `app/src/test/java/com/gizzi/web/` contains JUnit tests.
- `docker-compose.yml` and `app/Dockerfile` define containerized builds and local runtime.

## Build, Test, and Development Commands
Run commands from `app/` unless noted.
- `./gradlew clean build`: Compile and run all tests.
- `./gradlew test`: Execute the JUnit test suite.
- `./gradlew bootRun`: Start the application locally on port 8080.
- `./gradlew bootJar`: Build the runnable Spring Boot JAR under `app/build/libs/`.
- `docker compose up --build`: Build and run the container via `docker-compose.yml` (port 8080).

## Coding Style & Naming Conventions
- Java 21 with Spring Boot; follow standard Java conventions (4-space indentation, `UpperCamelCase` classes, `lowerCamelCase` methods/fields, `UPPER_SNAKE_CASE` constants).
- Package naming follows `com.gizzi.web`.
- Keep files focused; prefer one public class per file.
- No formatter or linter is currently configured—keep code consistent with existing style.

## Testing Guidelines
- Tests use JUnit 5 (`@Test`, `@SpringBootTest`).
- Name tests with `*Tests` or `*Test` and keep them under `app/src/test/java/...`.
- Run locally with `./gradlew test` before opening a PR.

## Commit & Pull Request Guidelines
- No commit history exists yet, so no established message convention.
- Use clear, present-tense commit summaries (e.g., "Add member lookup endpoint").
- PRs should include a short description, testing notes, and any relevant screenshots or logs when behavior changes.

## Security & Configuration Tips
- Store environment-specific values in `app/src/main/resources/application.properties` and override via environment variables when deploying.
- Avoid committing secrets; use `.env` or your deployment platform’s secret manager when needed.

## 개발시 코딩 포맷 관련 요구 사항
- 나는 데이터 형과 변수 등등을 동일한 열에 배치할 수 있게 공백을 맞추는걸 선호해
(예시) 원하지 않는 방식
  id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
  email VARCHAR(255) NOT NULL,
(예시) 원하는 방식
  id        CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
  email     VARCHAR(255)    NOT NULL,

