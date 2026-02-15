# API 설계 규칙

## URL 설계 원칙

### 기본 규칙
- RESTful 리소스 중심 URL (`/users`, `/groups/{id}`)
- 복수형 명사 사용 (`/users`, `/groups`, `/modules`)
- 소문자 + 하이픈(kebab-case) 사용 (`/auth/signup`, `/group-invites`)
- URL에 동사 사용 금지 — HTTP 메서드로 행위 표현
- 최대 3단계 중첩까지 허용 (`/groups/{id}/members/{memberId}`)

### HTTP 메서드

| 메서드 | 용도 | 예시 |
|--------|------|------|
| GET | 조회 (단건/목록) | `GET /users/{id}`, `GET /groups` |
| POST | 생성, 비CRUD 행위 | `POST /auth/login`, `POST /groups` |
| PUT | 전체 수정 | `PUT /users/{id}` |
| PATCH | 부분 수정 | `PATCH /users/{id}/status` |
| DELETE | 삭제 | `DELETE /groups/{id}` |

### 계획된 API 엔드포인트

```
# 인증/회원
POST   /auth/signup          # 회원가입
POST   /auth/login           # 로그인
POST   /auth/refresh         # 토큰 갱신
POST   /auth/logout          # 로그아웃

# 그룹
POST   /groups               # 그룹 생성
GET    /groups/{id}          # 그룹 조회
POST   /groups/{id}/invites  # 그룹 초대
POST   /groups/{id}/members  # 그룹 멤버 등록

# 모듈
POST   /modules                          # 모듈 등록
GET    /modules                          # 모듈 목록
POST   /modules/{code}/instances         # 모듈 인스턴스 생성
GET    /modules/{code}/instances/{id}    # 인스턴스 조회

# 권한
POST   /permissions/grant    # 권한 부여
POST   /permissions/revoke   # 권한 회수
GET    /permissions/check    # 권한 확인

# 메뉴 관리 (admin-api)
POST   /menus                # 메뉴 항목 생성
GET    /menus                # 메뉴 트리 전체 조회
PUT    /menus/{id}           # 메뉴 항목 수정
DELETE /menus/{id}           # 메뉴 항목 삭제
PATCH  /menus/{id}/order     # 메뉴 정렬 순서 변경
PATCH  /menus/{id}/toggle    # 메뉴 활성화/비활성화

# 메뉴 조회 (user-api)
GET    /menus/me             # 현재 사용자 권한 기반 메뉴 트리 조회

# Slug 기반 동적 라우팅 (user-api)
GET    /resolve/{module-slug}                  # SINGLE 모듈 slug → 모듈 정보
GET    /resolve/{module-slug}/{instance-slug}   # MULTI 모듈 slug → 인스턴스 정보
```

## Slug 기반 동적 라우팅

### 개요
모듈과 인스턴스에 slug를 부여하여 프론트엔드 URL을 동적으로 구성한다.

### URL 패턴

| 모듈 타입 | URL 패턴 | 예시 |
|-----------|----------|------|
| SINGLE | `/{module-slug}` | `/dashboard`, `/settings` |
| MULTI | `/{module-slug}/{instance-slug}` | `/board/notice`, `/wiki/dev-guide` |

### Slug 규칙
- 소문자 영문 + 숫자 + 하이픈만 허용 (`^[a-z0-9]+(-[a-z0-9]+)*$`)
- 최소 2자, 최대 50자
- 모듈 slug: `tb_modules` 내에서 유니크
- 인스턴스 slug: 동일 모듈 내에서 유니크 (`module_code` + `slug` 복합 유니크)

### 프론트엔드 연동 흐름

```
1. 사용자가 /board/notice 접근
2. 프론트엔드 → GET /resolve/board/notice
3. 백엔드 → module_code='board', instance_slug='notice' 조회
4. 응답: 모듈 정보 + 인스턴스 정보 + 사용자 권한
5. 프론트엔드 → 해당 모듈 컴포넌트 렌더링
```

### Resolve API 응답 구조

```json
{
  "success": true,
  "data": {
    "module": {
      "code": "board",
      "name": "게시판",
      "slug": "board",
      "type": "MULTI"
    },
    "instance": {
      "instanceId": "uuid-...",
      "name": "공지사항",
      "slug": "notice",
      "description": "공지사항 게시판"
    },
    "permissions": {
      "post": ["read"],
      "comment": ["read", "write"]
    }
  }
}
```

프론트엔드에서 `permissions.post.includes('write')` 체크로 UI 동적 제어.

### 메뉴 조회 API 응답 구조 (`GET /menus/me`)

```json
{
  "success": true,
  "data": [
    {
      "id": "menu-uuid-1",
      "name": "커뮤니티",
      "icon": "message-square",
      "menuType": "SEPARATOR",
      "children": [
        {
          "id": "menu-uuid-2",
          "name": "공지사항",
          "icon": "megaphone",
          "menuType": "MODULE",
          "url": "/board/notice",
          "permissions": {
            "post": ["read"],
            "comment": ["read", "write"]
          },
          "children": []
        },
        {
          "id": "menu-uuid-3",
          "name": "자유게시판",
          "menuType": "MODULE",
          "url": "/board/free",
          "permissions": {
            "post": ["read", "write", "reply"],
            "comment": ["read", "write", "anonymous"]
          },
          "children": []
        }
      ]
    }
  ]
}
```

- MODULE 타입: `url`은 모듈/인스턴스 slug에서 자동 생성, `permissions`는 리소스별 액션 목록
- LINK 타입: `url`은 `custom_url`, `permissions` 없음
- SEPARATOR 타입: `url` 없음, `children`으로 하위 메뉴 그룹핑

## 공통 응답 포맷

모든 API 응답은 `ApiResponse<T>` 래퍼를 사용한다.

### 성공 응답

```json
// 데이터 있는 응답
{
  "success": true,
  "data": { ... }
}

// 데이터 없는 응답 (생성, 삭제 등)
{
  "success": true
}
```

### 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "AUTH_UNAUTHORIZED",
    "message": "인증이 필요합니다"
  }
}
```

### null 필드 처리
- `@JsonInclude(JsonInclude.Include.NON_NULL)` 적용
- 성공 시 `error` 필드 제외, 에러 시 `data` 필드 제외

## 에러 코드 체계

### 코드 형식: `{카테고리}_{서술적_이름}`

| 카테고리 | 접두사 | 용도 |
|----------|--------|------|
| 공통 | `COM_` | 서버 오류, 입력 검증, 404, 405 |
| 인증 | `AUTH_` | 인증/인가 관련 |
| 사용자 | `USER_` | 회원 관련 비즈니스 오류 |
| 그룹 | `GROUP_` | 그룹 관련 비즈니스 오류 |
| 모듈 | `MODULE_` | 모듈/인스턴스 관련 오류 |
| 메뉴 | `MENU_` | 메뉴 관련 오류 |

### 현재 정의된 에러 코드

| 코드 | 메시지 | HTTP 상태 |
|------|--------|-----------|
| `COM_INTERNAL_ERROR` | 서버 내부 오류 | 500 |
| `COM_INVALID_INPUT` | 유효하지 않은 입력값 | 400 |
| `COM_RESOURCE_NOT_FOUND` | 리소스를 찾을 수 없음 | 404 |
| `COM_METHOD_NOT_ALLOWED` | 허용되지 않은 메서드 | 405 |
| `AUTH_UNAUTHORIZED` | 인증이 필요합니다 | 401 |
| `AUTH_ACCESS_DENIED` | 접근 권한이 없습니다 | 403 |
| `USER_DUPLICATE_ID` | 이미 사용 중인 아이디입니다 | 409 |
| `USER_DUPLICATE_EMAIL` | 이미 사용 중인 이메일입니다 | 409 |
| `USER_NOT_FOUND` | 사용자를 찾을 수 없습니다 | 404 |

### 에러 코드 추가 규칙
- `ErrorCode` enum에 정의 후 `BusinessException`으로 throw
- 새 에러 코드 추가 시 `{카테고리}_{서술적_이름}` 형식 (`USER_DUPLICATE_ID`)
- HTTP 상태 코드와 반드시 매핑

## 요청/응답 DTO 규칙

### DTO 네이밍

| 유형 | 패턴 | 예시 |
|------|------|------|
| 요청 | `{행위}{도메인}Request` | `CreateGroupRequest`, `LoginRequest` |
| 응답 | `{도메인}{상세}Response` | `UserProfileResponse`, `GroupDetailResponse` |
| 목록 응답 | `{도메인}ListResponse` | `GroupListResponse` |

### DTO 위치
- **공통 DTO**: `core/common/dto/` (ApiResponse, ErrorDetail, PageResponse 등)
- **도메인 DTO**: `core/domain/{도메인}/dto/` (각 도메인별 요청/응답)

### 검증 (Bean Validation)
- 요청 DTO에 `@Valid` 검증 적용
- 검증 실패 시 `GlobalExceptionHandler`가 400 + `COM_INVALID_INPUT` 반환
- 주요 어노테이션: `@NotBlank`, `@Email`, `@Size`, `@Pattern`

```java
// 예시: 회원가입 요청 DTO
@Getter
@Builder
public class SignupRequest {
    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 50, message = "아이디는 4~50자여야 합니다")
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다")
    private String password;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
}
```

## 페이징 규칙

### 요청 파라미터

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| `page` | 0 | 페이지 번호 (0-based) |
| `size` | 20 | 페이지 크기 |
| `sort` | - | 정렬 기준 (`createdAt,desc`) |

### 페이징 응답 구조

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

## 컨트롤러 작성 규칙

```java
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
public class GroupController {

    // 서비스 의존성 주입 (생성자 주입)
    private final GroupService groupService;

    // 그룹 생성 API
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {
        // 서비스 호출 후 응답 래핑
        GroupResponse response = groupService.createGroup(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(response));
    }
}
```

### 원칙
- 컨트롤러는 요청 검증 + 서비스 호출 + 응답 래핑만 담당
- 비즈니스 로직은 반드시 Service 레이어에 위치
- `@Valid`를 통한 요청 검증 필수
- 생성 성공 시 `201 Created`, 조회는 `200 OK`, 삭제는 `204 No Content`

## 프론트엔드 API 통신 규칙

### 요청 형식: JSON 필수
- 프론트엔드에서 백엔드로의 모든 요청은 **`Content-Type: application/json`** 으로 전송
- URL-encoded form 전송(`application/x-www-form-urlencoded`, `multipart/form-data`) 사용 금지
  - 예외: 파일 업로드 시에만 `multipart/form-data` 허용
- 백엔드 컨트롤러는 `@RequestBody`로 JSON 수신, `@ModelAttribute` 사용 금지

### Next.js 데이터 요청 패턴
- 클라이언트 컴포넌트에서 **표준 `fetch` API**로 JSON 요청 (저수준 API 사용 금지)
- React Server Action(`"use server"`)은 **백엔드 API 호출 용도로 사용하지 않음**
  - Server Action은 브라우저 → Next.js 서버 간 FormData(multipart) 프로토콜을 사용하므로 JSON 원칙에 위배
- Next.js `rewrites`로 `/api/**` 경로를 백엔드로 프록시하여 CORS 우회
- 공통 API 유틸리티(`src/lib/api.ts`)를 통해 요청

```typescript
// next.config.ts — 백엔드 프록시 설정
async rewrites() {
  return [{ source: "/api/:path*", destination: "http://localhost:6100/:path*" }];
}

// src/lib/api.ts — 공통 JSON 요청 유틸리티
export async function apiPost<T>(path: string, body: unknown): Promise<ApiResponse<T>> {
  const res = await fetch(`/api${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return res.json();
}

// 컴포넌트에서 사용
const json = await apiPost<SignupResponse>("/auth/signup", { userId, password, username, email });
```

### 포트 제한 주의
- Node.js `fetch`(undici)는 포트 6000 등 unsafe port를 차단함
- 백엔드 서비스 포트는 unsafe port 목록을 피해서 설정할 것
  - 참고: https://fetch.spec.whatwg.org/#port-blocking
