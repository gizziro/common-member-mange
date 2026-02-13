# 코딩 컨벤션

## 언어: 한국어 원칙

- **모든 주석은 한국어로 작성**
- 코드 한 줄에 주석 한 줄 수준으로 세세하게 작성
- 클래스/메서드/필드 위에 한국어 설명 주석 필수
- 변수명·메서드명·클래스명은 영어, 주석·문서는 한국어

```java
// 사용자 이메일 인증 여부 확인
boolean isEmailVerified = user.getEmailVerified();

// 인증되지 않은 사용자는 접근 차단
if (!isEmailVerified) {
    throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 인증이 필요합니다");
}
```

## Java 컨벤션

### 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | `UpperCamelCase` | `UserService`, `GroupController` |
| 메서드 | `lowerCamelCase` | `findByEmail()`, `createGroup()` |
| 필드/변수 | `lowerCamelCase` | `userId`, `groupName` |
| 상수 | `UPPER_SNAKE_CASE` | `MAX_LOGIN_ATTEMPTS`, `TOKEN_EXPIRY` |
| 패키지 | 소문자 | `com.gizzi.core.common.exception` |
| Enum 값 | `UPPER_SNAKE_CASE` | `ACTIVE`, `INTERNAL_SERVER_ERROR` |

### 패키지 구조

```
com.gizzi.core/                    # core 모듈
  common/
    dto/                           # 공통 DTO (ApiResponse, ErrorDetail 등)
    exception/                     # 공통 예외 (ErrorCode, BusinessException 등)
    config/                        # 공통 설정
    util/                          # 공통 유틸리티
  domain/
    user/                          # 사용자 도메인
      entity/                      #   JPA 엔티티
      repository/                  #   리포지토리
      service/                     #   서비스
      dto/                         #   요청/응답 DTO
    group/                         # 그룹 도메인
    menu/                          # 메뉴 관리 도메인
    auth/                          # 인증 도메인
  module/                          # 모듈 시스템 프레임워크
    ModuleDefinition.java          #   모듈 등록 인터페이스
    ModuleRegistry.java            #   모듈 자동 발견/등록
    SlugResolver.java              #   slug → 모듈/인스턴스 매핑
    PermissionChecker.java         #   권한 체크 유틸리티

com.gizzi.module.board/            # 게시판 기능 모듈
  BoardModuleDefinition.java       #   모듈 메타데이터 (이름, slug, 리소스별 권한)
  entity/                          #   게시판 엔티티 (Post, Comment 등)
  repository/                      #   리포지토리
  service/                         #   서비스 (admin/user 공용 비즈니스 로직)
  dto/                             #   공통 DTO
  admin/                           #   관리자 전용 (@ConditionalOnProperty api-type=admin)
    controller/
    dto/
  api/                             #   사용자 전용 (@ConditionalOnProperty api-type=user)
    controller/
    dto/

com.gizzi.admin/                   # admin-api 모듈
  config/                          # 관리자 전용 설정 (Security 등)
  controller/                      # 관리자 API 컨트롤러 (core 도메인용)
    user/
    group/
    menu/
    system/

com.gizzi.user/                    # user-api 모듈
  config/                          # 사용자 전용 설정 (Security 등)
  controller/                      # 사용자 API 컨트롤러 (core 도메인용)
    auth/
    profile/
    group/
    menu/
```

### 포매팅

- 들여쓰기: **탭 (Tab)** 사용 (IntelliJ 기본 설정)
- 파일당 public 클래스 하나
- 선언 시 **열 정렬(Column-aligned)** 선호:

```java
// 좋은 예: 열 정렬
private final String     code;
private final String     message;
private final HttpStatus httpStatus;

// 나쁜 예: 정렬 없음
private final String code;
private final String message;
private final HttpStatus httpStatus;
```

### Lombok 사용 규칙

- Lombok은 `compileOnly`로 설정됨 — 보일러플레이트 제거 용도로 적극 활용
- 권장 어노테이션:

| 어노테이션 | 용도 | 적용 대상 |
|-----------|------|----------|
| `@Getter` | Getter 자동 생성 | Entity, DTO |
| `@Builder` | Builder 패턴 | DTO, 복잡한 생성 객체 |
| `@AllArgsConstructor` | 전체 필드 생성자 | Enum, DTO |
| `@NoArgsConstructor(access = PROTECTED)` | JPA 기본 생성자 | Entity |
| `@RequiredArgsConstructor` | final 필드 생성자 주입 | Service, Controller |
| `@Slf4j` | 로깅 | Service, Controller, Handler |

- **지양 사항**:
  - `@Data` 사용 금지 (equals/hashCode 자동 생성이 JPA에서 문제 유발)
  - `@Setter` 사용 금지 (불변 객체 선호, Entity는 비즈니스 메서드로 상태 변경)
  - `@ToString` Entity에 직접 사용 금지 (연관 엔티티 순환 참조 위험)

### 클래스 구조 순서

```java
// 1. static 상수
// 2. 필드
// 3. 생성자
// 4. public 메서드
// 5. protected/package-private 메서드
// 6. private 메서드
```

### 모듈 권한 문자열 컨벤션

권한은 `{MODULE}_{RESOURCE}_{ACTION}` 형식의 UPPER_SNAKE_CASE 플랫 문자열로 표현:

```
BOARD_POST_READ          # 게시판 - 게시글 읽기
BOARD_POST_WRITE         # 게시판 - 게시글 작성
BOARD_POST_MODIFY        # 게시판 - 게시글 수정
BOARD_POST_DELETE        # 게시판 - 게시글 삭제
BOARD_POST_REPLY         # 게시판 - 게시글 답글
BOARD_COMMENT_READ       # 게시판 - 댓글 읽기
BOARD_COMMENT_WRITE      # 게시판 - 댓글 작성
BOARD_COMMENT_ANONYMOUS  # 게시판 - 댓글 익명 작성
BLOG_POST_PUBLISH        # 블로그 - 글 발행
```

- DB에는 3컬럼(`module_code`, `resource`, `action`)으로 구조화 저장
- 런타임에서 `UPPER(module_code) + "_" + UPPER(resource) + "_" + UPPER(action)` 으로 조합
- 코드에서 권한 체크: `permissionChecker.hasPermission(userId, instanceId, "BOARD_POST_WRITE")`

## TypeScript/React 컨벤션 (Frontend)

### 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 컴포넌트 | `PascalCase` | `UserProfile`, `LoginForm` |
| 함수/변수 | `camelCase` | `handleSubmit`, `userName` |
| 타입/인터페이스 | `PascalCase` | `UserResponse`, `LoginRequest` |
| 상수 | `UPPER_SNAKE_CASE` | `API_BASE_URL` |
| 파일 (컴포넌트) | `PascalCase.tsx` | `UserProfile.tsx` |
| 파일 (유틸리티) | `camelCase.ts` | `apiClient.ts` |

### 기본 원칙

- TypeScript strict 모드 사용
- `any` 타입 사용 금지 — 명시적 타입 정의
- 컴포넌트는 함수형 컴포넌트 + React Hooks 사용
- API 응답 타입은 백엔드 `ApiResponse<T>` 구조와 일치시킬 것

## Git 커밋 컨벤션

### 커밋 메시지 형식

```
<제목> (한국어, 50자 이내)

<본문> (선택, 변경 사항 요약)
- 항목1
- 항목2
```

### 제목 동사 규칙

| 동사 | 의미 |
|------|------|
| 추가 | 완전히 새로운 기능/파일 |
| 수정 | 기존 기능 개선/변경 |
| 수정(fix) | 버그 수정 |
| 제거 | 코드/파일 삭제 |
| 리팩토링 | 동작 변경 없이 구조 개선 |
| 설정 | 빌드/환경/설정 변경 |
