# 빌드 인수: 빌드할 API 모듈 이름 (admin-api 또는 user-api)
ARG MODULE_NAME

# Stage 1: Gradle 빌드
FROM gradle:8.14.3-jdk21 AS builder

ARG MODULE_NAME

WORKDIR /workspace
COPY . /workspace

# 지정된 모듈의 bootJar 태스크 실행
RUN ./gradlew :${MODULE_NAME}:clean :${MODULE_NAME}:bootJar --no-daemon

# Stage 2: 실행 이미지
FROM eclipse-temurin:21-jre

ARG MODULE_NAME

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /workspace/${MODULE_NAME}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
