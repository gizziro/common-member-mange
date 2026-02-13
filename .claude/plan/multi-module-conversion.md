# 멀티모듈 프로젝트 전환 계획

## Context

현재 `backend/`는 단일 모듈 Spring Boot 프로젝트(빈 스켈레톤)이다. NOTICE.MD에 정의된 `core` / `admin-api` / `user-api` 멀티모듈 구조로 전환하고, Next.js 프론트엔드 프로젝트 2개를 초기화한다.

## 최종 디렉토리 구조

```
common-member-mange/
  docker-compose.yml               # 수정: profiles 기반 멀티 서비스
  db/init/                         # 기존 유지
  backend/
    .dockerignore                  # 신규: Docker 빌드 최적화
    build.gradle                   # 수정: 루트 (plugins apply false)
    settings.gradle                # 수정: include 'core', 'admin-api', 'user-api'
    Dockerfile                     # 수정: ARG MODULE_NAME 기반
    gradlew, gradlew.bat, gradle/  # 기존 유지
    buildSrc/                      # 신규: 컨벤션 플러그인
      build.gradle
      src/main/groovy/
        com.gizzi.web.java-conventions.gradle
    core/                          # 신규: 공통 모듈 (라이브러리)
      build.gradle
      src/main/java/com/gizzi/web/core/
      src/test/java/com/gizzi/web/core/
    admin-api/                     # 신규: 관리자 API (포트 5000)
      build.gradle
      src/main/java/com/gizzi/web/admin/
        AdminApiApplication.java
      src/main/resources/application.properties
      src/test/java/com/gizzi/web/admin/
    user-api/                      # 신규: 사용자 API (포트 6000)
      build.gradle
      src/main/java/com/gizzi/web/user/
        UserApiApplication.java
      src/main/resources/application.properties
      src/test/java/com/gizzi/web/user/
  frontend/
    admin/                         # 신규: Next.js (관리자)
    user/                          # 신규: Next.js (사용자)
```

---

## 1단계: Gradle 멀티모듈 구성

### 1-1. `backend/buildSrc/` 컨벤션 플러그인 생성

공통 Java 빌드 설정(Java 21, Lombok, 저장소, 테스트)을 한곳에서 관리.

**`backend/buildSrc/build.gradle`**
```groovy
plugins {
    id 'groovy-gradle-plugin'
}
```

**`backend/buildSrc/src/main/groovy/com.gizzi.web.java-conventions.gradle`**
```groovy
plugins {
    id 'java'
}

group   = 'com.gizzi'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly         'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation  'org.springframework.boot:spring-boot-starter-webmvc-test'
    testRuntimeOnly     'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### 1-2. `backend/settings.gradle` 수정

```groovy
rootProject.name = 'member-mange'

include 'core'
include 'admin-api'
include 'user-api'
```

### 1-3. `backend/build.gradle` (루트) 수정

```groovy
plugins {
    id 'org.springframework.boot'        version '4.0.2' apply false
    id 'io.spring.dependency-management' version '1.1.7' apply false
}
```

### 1-4. `backend/core/build.gradle`

```groovy
plugins {
    id 'com.gizzi.web.java-conventions'
    id 'io.spring.dependency-management'
}

dependencyManagement {
    imports {
        mavenBom 'org.springframework.boot:spring-boot-dependencies:4.0.2'
    }
}

description = 'Core - 공통 도메인/엔티티/리포지토리/서비스/권한/예외처리'
```

> core는 Spring Boot 플러그인 미적용 (실행 불가 라이브러리). BOM만 임포트하여 의존성 버전 관리. 구체적 Spring 의존성(JPA, Security 등)은 기능 구현 시 추가 예정.

### 1-5. `backend/admin-api/build.gradle`

```groovy
plugins {
    id 'com.gizzi.web.java-conventions'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

description = 'Admin API - 시스템 설정/감사로그/관리자 기능 (포트 5000)'

dependencies {
    implementation project(':core')
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

### 1-6. `backend/user-api/build.gradle`

```groovy
plugins {
    id 'com.gizzi.web.java-conventions'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

description = 'User API - 로그인/회원/모듈/그룹/사용자 기능 (포트 6000)'

dependencies {
    implementation project(':core')
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

---

## 2단계: 소스 코드 구조

### 2-1. core 모듈 디렉토리 생성

빈 디렉토리만 생성 (소스 코드는 기능 구현 시 추가):
- `backend/core/src/main/java/com/gizzi/web/core/` (`.gitkeep`)
- `backend/core/src/test/java/com/gizzi/web/core/` (`.gitkeep`)

### 2-2. admin-api Application 클래스

**`backend/admin-api/src/main/java/com/gizzi/web/admin/AdminApiApplication.java`**
```java
package com.gizzi.web.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 관리자 API 애플리케이션 진입점
@SpringBootApplication(scanBasePackages = {"com.gizzi.web.core", "com.gizzi.web.admin"})
public class AdminApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
```

**`backend/admin-api/src/main/resources/application.properties`**
```properties
spring.application.name=Admin API
server.port=5000
management.endpoints.web.exposure.include=health,info
```

### 2-3. user-api Application 클래스

**`backend/user-api/src/main/java/com/gizzi/web/user/UserApiApplication.java`**
```java
package com.gizzi.web.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 사용자 API 애플리케이션 진입점
@SpringBootApplication(scanBasePackages = {"com.gizzi.web.core", "com.gizzi.web.user"})
public class UserApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }
}
```

**`backend/user-api/src/main/resources/application.properties`**
```properties
spring.application.name=User API
server.port=6000
management.endpoints.web.exposure.include=health,info
```

### 2-4. 기존 소스 삭제

- `backend/src/` 디렉토리 전체 삭제 (WebServiceApplication.java 등)

---

## 3단계: Docker 구성

### 3-1. `backend/Dockerfile` 수정 (MODULE_NAME 인수 기반)

```dockerfile
ARG MODULE_NAME

FROM gradle:8.14.3-jdk21 AS builder
ARG MODULE_NAME
WORKDIR /workspace
COPY . /workspace
RUN ./gradlew :${MODULE_NAME}:clean :${MODULE_NAME}:bootJar --no-daemon

FROM eclipse-temurin:21-jre
ARG MODULE_NAME
WORKDIR /app
COPY --from=builder /workspace/${MODULE_NAME}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 3-2. `backend/.dockerignore` 신규 생성

```
.gradle/
**/build/
.idea/
*.iml
```

### 3-3. `docker-compose.yml` 수정 (profiles 기반)

```yaml
services:
  admin-api:
    build:
      context: ./backend
      args:
        MODULE_NAME: admin-api
    container_name: member-mange-admin-api
    ports:
      - "5000:5000"
    depends_on:
      - db
    profiles: [admin, full]

  user-api:
    build:
      context: ./backend
      args:
        MODULE_NAME: user-api
    container_name: member-mange-user-api
    ports:
      - "6000:6000"
    depends_on:
      - db
    profiles: [user, full]

  db:
    image: mysql:8.0
    container_name: member-mange-db
    restart: unless-stopped
    ports:
      - "13306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: app_db
      MYSQL_USER: app_user
      MYSQL_PASSWORD: app_password
    volumes:
      - mysql_data:/var/lib/mysql
      - ./db/init:/docker-entrypoint-initdb.d:ro

volumes:
  mysql_data:
```

**사용법:**
- `docker compose --profile full up --build` → 전체 (admin + user + db)
- `docker compose --profile admin up --build` → 관리자 API + db
- `docker compose --profile user up --build` → 사용자 API + db
- `docker compose up -d db` → DB만 (로컬 IntelliJ 개발용)

---

## 4단계: 프론트엔드 초기화

- `frontend/.gitkeep` 삭제
- `npx create-next-app@latest frontend/admin` (TypeScript, Tailwind, ESLint, App Router, src-dir)
- `npx create-next-app@latest frontend/user` (동일 옵션)

> 초기화만 수행. Docker Compose 연동은 추후 추가.

---

## 5단계: 문서 업데이트

`CLAUDE.md`의 Build & Run Commands 섹션과 Architecture 섹션을 멀티모듈 구조에 맞게 갱신.

---

## 검증 방법

```bash
# backend/ 디렉토리에서 실행
./gradlew clean build                  # 전체 빌드 성공 확인
./gradlew :admin-api:bootRun           # 관리자 API 실행 → localhost:5000/actuator/health
./gradlew :user-api:bootRun            # 사용자 API 실행 → localhost:6000/actuator/health

# 프론트엔드 확인
cd frontend/admin && npm run dev       # Next.js 관리자 dev server 구동 확인
cd frontend/user && npm run dev        # Next.js 사용자 dev server 구동 확인
```
