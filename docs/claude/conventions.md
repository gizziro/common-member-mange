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

### 주석 및 구분선 스타일

프로젝트 전체에서 일관된 주석 스타일을 적용한다.

#### 1. 구분선 길이: 120자 통일

모든 구분선은 `//` 포함 **120자**로 통일한다.

| 구분선 | 구성 | 용도 |
|--------|------|------|
| `//==...==` | `//` + `=` × 118 = 120자 | 핵심 public 메서드 |
| `//--...--` | `//` + `-` × 118 = 120자 | 보조 메서드, 필드 그룹, 생성자, getter |

메서드 내부 들여쓰기된 구분선도 **탭 + 120자**로 동일 길이를 유지한다.

```java
//======================================================================================================================
// 핵심 비즈니스 메서드 (클래스 레벨)
//======================================================================================================================

	//----------------------------------------------------------------------------------------------------------------------
	// 1-탭 들여쓰기 필드/보조 메서드 (클래스 멤버 레벨)
	//----------------------------------------------------------------------------------------------------------------------

		//----------------------------------------------------------------------------------------------------------------------
		// 2-탭 들여쓰기 메서드 내부 블록 (메서드 본문)
		//----------------------------------------------------------------------------------------------------------------------
```

#### 2. 클래스 레벨 구분선 사용 규칙

| 구분선 | 용도 | 예시 |
|--------|------|------|
| `//==` (120자) | 핵심 public 메서드 (비즈니스 로직, 주요 기능) | `login()`, `createPost()`, `generateAccessToken()` |
| `//--` (120자) | 보조 메서드, 필드 그룹, 생성자, getter 성격 메서드 | `getUserPk()`, 필드 선언부, 생성자 |

#### 3. 필드 그룹 헤더

관련 필드를 `// [ 섹션명 ]` 형식으로 묶어 표시한다:

```java
	//----------------------------------------------------------------------------------------------------------------------
	// [ JWT 토큰 관련 변수 ]
	//----------------------------------------------------------------------------------------------------------------------
	private final SecretKey      secretKey;          // JWT 서명에 사용할 비밀키
	private final JwtProperties  jwtProperties;      // JWT 설정 프로퍼티
```

#### 4. 인라인 필드 주석

필드 선언 끝에 **탭 정렬**로 `// 설명` 을 배치한다:

```java
	private final String     code;          // 에러 코드 문자열
	private final String     message;       // 사용자 노출 메시지
	private final HttpStatus httpStatus;    // HTTP 상태 코드
```

#### 5. 메서드 내부 블록 주석

코드 블록 앞에 들여쓰기된 `//--` 구분선으로 의미 단위를 구분한다:

```java
	public String generateAccessToken(String userPk, String userId, String sessionId)
	{
		//----------------------------------------------------------------------------------------------------------------------
		// 현재 시각 기준 만료 시각 계산
		//----------------------------------------------------------------------------------------------------------------------
		Instant now    = Instant.now();
		Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpiration());

		//----------------------------------------------------------------------------------------------------------------------
		// Access Token JWT 빌드
		//----------------------------------------------------------------------------------------------------------------------
		return Jwts.builder()
			.subject(userPk)
			.compact();
	}
```

#### 6. Allman 브레이스 스타일

여는 `{`를 새 줄에 배치한다 (클래스, 메서드, try/catch, if/else):

```java
// 클래스 선언
public class JwtTokenProvider
{
	// 메서드 선언
	public String generateToken()
	{
		try
		{
			return token;
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	// 조건문
	if (condition)
	{
		doSomething();
	}
	else
	{
		doOther();
	}
}
```

#### 7. 대입문 탭 정렬

관련 대입문들의 `=` 위치를 탭으로 정렬한다:

```java
	this.jwtProperties  = jwtProperties;
	this.settingService = settingService;
	this.secretKey      = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
```

#### 8. 메서드 선언부 반환값-메서드명 탭 정렬

인터페이스나 같은 클래스 내 여러 메서드 선언이 나란히 있을 경우, **반환 타입과 메서드명 사이에 탭**을 넣어 메서드명이 시각적으로 빠르게 식별되도록 한다. 정확한 탭 수는 고정하지 않되, 주변 선언들과 함께 봤을 때 읽기 편한 정도의 간격을 유지한다:

```java
	// 같은 그룹 내 반환 타입-메서드명 정렬
	Optional<UserEntity> 		findByUserId(String userId);
	Optional<UserEntity> 		findByEmail(String email);

	boolean 		existsByUserId(String userId);
	boolean 		existsByEmail(String email);
	boolean 		existsByEmailAndIdNot(String email, String id);

	List<UserEntity> 		findSmsEligibleUsers();
	long 	countSmsEligibleUsers();
```

#### 9. 클래스 주석: `//` 스타일

클래스 선언 바로 위에 Javadoc(`/** */`) 대신 `//` 주석으로 클래스 설명을 작성한다:

```java
// JWT 토큰 생성, 파싱, 검증을 담당하는 컴포넌트
// Access Token과 Refresh Token의 생성 및 클레임 추출을 처리한다
@Slf4j
@Component
public class JwtTokenProvider
{
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
