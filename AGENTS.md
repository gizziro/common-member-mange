# Repository Guidelines

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
