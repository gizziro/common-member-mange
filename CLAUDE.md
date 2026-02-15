# CLAUDE.md

이 파일은 Claude Code가 이 프로젝트의 코드를 작성할 때 따라야 할 지침을 제공합니다.

## 핵심 원칙

- **모든 주석은 한국어로 작성** — 코드 한 줄에 주석 한 줄 수준으로 세세하게 작성
- 변수명/메서드명/클래스명은 영어, 주석/문서/커밋 메시지는 한국어
- Lombok을 적극 활용하되 `@Data`, `@Setter`는 사용 금지
- 컨트롤러는 요청 검증 + 서비스 호출만, 비즈니스 로직은 Service 레이어에 위치
- 모든 API 응답은 `ApiResponse<T>` 래퍼 사용 (core 모듈의 공통 DTO)
- 비즈니스 예외는 `BusinessException` + `ErrorCode` enum으로 처리

## 상세 지침

@docs/claude/environment.md
@docs/claude/goals.md
@docs/claude/conventions.md
@docs/claude/api-design.md
@docs/claude/database.md
@docs/claude/security.md
@docs/claude/redis.md
@docs/claude/testing.md
