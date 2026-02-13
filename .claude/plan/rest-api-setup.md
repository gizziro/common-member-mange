# 패키지 리네이밍 + REST API 기초 셋팅 계획

## Context

현재 패키지(`com.gizzi.web.*`)가 불필요하게 길다. `com.gizzi.*`로 단축하고, REST API 공통 기반(응답 래퍼, 예외처리)을 core 모듈에 구축한다.

---

## 1단계: 패키지 리네이밍

### 변경 사항

| 대상 | 변경 전 | 변경 후 |
|------|---------|---------|
| core | `com.gizzi.web.core` | `com.gizzi.core` |
| admin-api | `com.gizzi.web.admin` | `com.gizzi.admin` |
| user-api | `com.gizzi.web.user` | `com.gizzi.user` |
| 컨벤션 플러그인 ID | `com.gizzi.web.java-conventions` | `com.gizzi.java-conventions` |

### 수정 파일 목록

1. **buildSrc 컨벤션 플러그인 파일명 변경**
   - `backend/buildSrc/src/main/groovy/com.gizzi.web.java-conventions.gradle`
   - → `backend/buildSrc/src/main/groovy/com.gizzi.java-conventions.gradle`

2. **각 모듈 build.gradle의 플러그인 ID 변경** (3개 파일)
   - `backend/core/build.gradle`: `com.gizzi.web.java-conventions` → `com.gizzi.java-conventions`
   - `backend/admin-api/build.gradle`: 동일
   - `backend/user-api/build.gradle`: 동일

3. **AdminApiApplication.java 이동 + 수정**
   - 경로: `backend/admin-api/src/main/java/com/gizzi/web/admin/` → `backend/admin-api/src/main/java/com/gizzi/admin/`
   - package: `com.gizzi.admin`
   - scanBasePackages: `{"com.gizzi.core", "com.gizzi.admin"}`

4. **UserApiApplication.java 이동 + 수정**
   - 경로: `backend/user-api/src/main/java/com/gizzi/web/user/` → `backend/user-api/src/main/java/com/gizzi/user/`
   - package: `com.gizzi.user`
   - scanBasePackages: `{"com.gizzi.core", "com.gizzi.user"}`

5. **core 디렉토리 이동**
   - `backend/core/src/main/java/com/gizzi/web/core/` → `backend/core/src/main/java/com/gizzi/core/`
   - `backend/core/src/test/java/com/gizzi/web/core/` → `backend/core/src/test/java/com/gizzi/core/`

6. **기존 `com/gizzi/web/` 디렉토리 삭제** (빈 디렉토리 정리)

---

## 2단계: REST API 기초 셋팅 (core 모듈)

### core에 구현하는 이유
- admin-api, user-api 모두 **동일한 응답 포맷** 필요
- `@RestControllerAdvice`를 core에 두면 scanBasePackages로 양쪽에서 자동 적용
- 변경 시 한 곳만 수정

### core/build.gradle 의존성 추가

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
```

### 생성할 파일 (com.gizzi.core 하위)

#### 2-1. `core/.../common/dto/ApiResponse.java` — 공통 응답 래퍼

```java
@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T       data;
    private final ErrorDetail error;

    public static <T> ApiResponse<T> ok(T data) { ... }
    public static ApiResponse<Void> ok() { ... }
    public static ApiResponse<Void> error(ErrorCode code) { ... }
    public static ApiResponse<Void> error(ErrorCode code, String message) { ... }
}
```

#### 2-2. `core/.../common/dto/ErrorDetail.java` — 에러 상세 DTO

#### 2-3. `core/.../common/exception/ErrorCode.java` — 에러 코드 enum (COM_xxx, AUTH_xxx)

#### 2-4. `core/.../common/exception/BusinessException.java` — 비즈니스 예외 기본 클래스

#### 2-5. `core/.../common/exception/GlobalExceptionHandler.java` — 전역 @RestControllerAdvice

### core 모듈 최종 패키지 구조

```
com.gizzi.core/
  common/
    dto/
      ApiResponse.java
      ErrorDetail.java
    exception/
      ErrorCode.java
      BusinessException.java
      GlobalExceptionHandler.java
```

---

## 3단계: CLAUDE.md 갱신

패키지 구조 변경 반영.

---

## 검증 방법

```bash
cd backend
./gradlew clean build              # 전체 빌드 성공 확인
./gradlew :admin-api:bootRun       # localhost:5000/actuator/health 확인
./gradlew :user-api:bootRun        # localhost:6000/actuator/health 확인
```
